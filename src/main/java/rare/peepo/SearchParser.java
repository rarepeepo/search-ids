package rare.peepo;

import java.util.ArrayList;
import java.util.List;

public final class SearchParser {

    private SearchParser() {}

    public static SearchQuery parse(String input) {
        if (input == null)
            input = "";

        var tokens = tokenize(input);
        var conditions = new ArrayList<SearchQuery.Condition>();
        var limit = 100;
        var page = 1;

        for (var raw : tokens) {
            if (raw.isEmpty())
                continue;

            var negated = raw.charAt(0) == '-';
            var token = negated ? raw.substring(1) : raw;
            if (token.isEmpty())
                continue;
            if (token.startsWith("limit:")) {
                try {
                    var v = token.substring("limit:".length());
                    var n = Integer.parseInt(v);
                    if (n >= 0)
                        limit = n;
                } catch (Exception ignored) {}
                // do not add as condition
                continue;
            } else if (token.startsWith("page:")) {
                try {
                    var v = token.substring("page:".length());
                    var n = Integer.parseInt(v);
                    if (n >= 1)
                        page = n;
                } catch (Exception ignored) {}
                // do not add as condition
                continue;
            }
            SearchQuery.Field field;
            String pattern;

            // JEI-compatible: @modid
            if (token.charAt(0) == '@') {
                field = SearchQuery.Field.MODID;
                pattern = token.substring(1);
            } else if (token.startsWith("id:")) {
                field = SearchQuery.Field.ID;
                pattern = token.substring("id:".length());
            } else if (token.startsWith("name:")) {
                field = SearchQuery.Field.NAME;
                pattern = token.substring("name:".length());
            } else if (token.startsWith("modid:")) {
                field = SearchQuery.Field.MODID;
                pattern = token.substring("modid:".length());
            } else if (token.startsWith("type:")) {
                field = SearchQuery.Field.TYPE;
                pattern = token.substring("type:".length());
            } else {
                // GENERAL: id + name
                field = SearchQuery.Field.GENERAL;
                pattern = token;
            }

            if (pattern.isEmpty())
                continue;
            conditions.add(new SearchQuery.Condition(field, pattern, negated));
        }
        return new SearchQuery(conditions, limit, page);
    }

    private static List<String> tokenize(String input) {
        var result = new ArrayList<String>();
        var sb = new StringBuilder();
        var inQuotes = false;

        for (var i = 0; i < input.length(); i++) {
            var c = input.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (Character.isWhitespace(c) && !inQuotes) {
                if (sb.length() > 0) {
                    result.add(sb.toString());
                    sb.setLength(0);
                }
            } else {
                sb.append(c);
            }
        }
        if (sb.length() > 0)
            result.add(sb.toString());
        return result;
    }
}