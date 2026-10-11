package com.pharmacy.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Small builder for optional WHERE conditions. Only fixed SQL fragments written
 * by the developer are appended; every user value travels as a bind parameter.
 */
public class Where {
    private final StringBuilder sql = new StringBuilder();
    private final List<Object> params = new ArrayList<>();

    public Where and(String condition, Object... values) {
        sql.append(sql.length() == 0 ? " WHERE " : " AND ").append('(').append(condition).append(')');
        params.addAll(Arrays.asList(values));
        return this;
    }

    public Where andIf(boolean apply, String condition, Object... values) {
        return apply ? and(condition, values) : this;
    }

    /** Adds "col LIKE ? OR col2 LIKE ? ..." for a free-text search term. */
    public Where search(String term, String... columns) {
        if (term == null || term.isBlank()) {
            return this;
        }
        String pattern = "%" + escapeLike(term.trim()) + "%";
        StringBuilder cond = new StringBuilder();
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            cond.append(i == 0 ? "" : " OR ").append(columns[i]).append(" LIKE ?");
            values[i] = pattern;
        }
        return and(cond.toString(), values);
    }

    public static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public String sql() {
        return sql.toString();
    }

    public List<Object> params() {
        return params;
    }
}
