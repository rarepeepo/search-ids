package searchids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SearchQuery {

    public enum Field {
        GENERAL, // id + name
        ID,
        NAME,
        MODID,
        TYPE
    }

    public static final class Condition {
        public final Field field;
        public final String pattern;
        public final boolean negated;

        public Condition(Field field, String pattern, boolean negated) {
            this.field = field;
            this.pattern = pattern;
            this.negated = negated;
        }

        @Override
        public String toString() {
            return (negated ? "-" : "") + field + ":" + pattern;
        }
    }

    private final List<Condition> conditions;
    private final int limit;
    private final int page;
    
    public SearchQuery(List<Condition> conditions, int limit, int page) {
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.limit = limit;
        this.page = page;
    }

    public List<Condition> getConditions() {
        return conditions;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public int getPage() {
        return page;
    }
}