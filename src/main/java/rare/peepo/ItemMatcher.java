package rare.peepo;

public final class ItemMatcher {

    private ItemMatcher() {}

    public static boolean matches(String id, String name, String modid, SearchQuery query) {
        if (id == null)
            id = "";
        if (name == null)
            name = "";
        if (modid == null)
            modid = "";

        for (SearchQuery.Condition cond : query.getConditions()) {
            if (cond.field == SearchQuery.Field.TYPE)
                continue;
            var match = matchesCondition(id, name, modid, cond);
            if (cond.negated) {
                if (match)
                    return false;
            } else {
                if (!match)
                    return false;
            }
        }
        return true;
    }

    private static boolean matchesCondition(String id, String name, String modid,
        SearchQuery.Condition cond) {
        var pattern = cond.pattern.toLowerCase();

        switch (cond.field) {
            case MODID:
                return matchSimple(pattern, modid);
            case ID:
                return matchSimple(pattern, id);
            case NAME:
                return matchSimple(pattern, name);
            case GENERAL:
            default:
                return matchSimple(pattern, id) || matchSimple(pattern, name);
        }
    }

    private static boolean matchSimple(String pattern, String value) {
        if (value == null)
            return false;
        pattern = pattern.toLowerCase();
        var v = value.toLowerCase();

        // If the pattern does NOT contain a namespace, only match
        // against the id path (part after ":") when the value is a full id
        if (pattern.indexOf(':') < 0) {
            var colon = v.indexOf(':');
            if (colon >= 0 && colon + 1 < v.length()) {
                v = v.substring(colon + 1);
            }
        }

        var startsWithStar = pattern.startsWith("*");
        var endsWithStar = pattern.endsWith("*");

        var core = pattern;
        if (startsWithStar)
            core = core.substring(1);
        if (endsWithStar && core.length() > 0)
            core = core.substring(0, core.length() - 1);

        if (core.isEmpty())
            return true;

        if (startsWithStar && endsWithStar) {
            return v.contains(core);
        } else if (startsWithStar) {
            return v.endsWith(core);
//        } else if (endsWithStar) {
//            return v.startsWith(core);
        } else {
            return v.contains(core);
        }
    }
}