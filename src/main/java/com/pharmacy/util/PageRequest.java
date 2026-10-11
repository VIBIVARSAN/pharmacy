package com.pharmacy.util;

import java.util.Map;

/**
 * Paging, sorting and free-text search parameters of a list request.
 * Sorting is only ever applied through a whitelist, so user input can never
 * reach an ORDER BY clause directly.
 */
public class PageRequest {
    public static final int DEFAULT_SIZE = 10;
    public static final int MAX_SIZE = 100;

    private final int page;
    private final int size;
    private final String sort;
    private final boolean descending;
    private final String query;

    public PageRequest(int page, int size, String sort, boolean descending, String query) {
        this.page = Math.max(1, page);
        this.size = Math.min(MAX_SIZE, Math.max(1, size));
        this.sort = sort;
        this.descending = descending;
        this.query = query == null || query.isBlank() ? null : query.trim();
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null, false, null);
    }

    /** Everything on one page (used by print-friendly report views). */
    public static PageRequest firstPage(int size) {
        return new PageRequest(1, size, null, false, null);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getOffset() {
        return (long) (page - 1) * size;
    }

    public String getSort() {
        return sort;
    }

    public boolean isDescending() {
        return descending;
    }

    public String getDir() {
        return descending ? "desc" : "asc";
    }

    public String getQuery() {
        return query;
    }

    public boolean hasQuery() {
        return query != null;
    }

    /**
     * Builds a safe ORDER BY expression.
     *
     * @param allowed      map of public sort key to SQL column expression
     * @param defaultOrder full default expression, e.g. "p.created_at DESC"
     */
    public String orderBy(Map<String, String> allowed, String defaultOrder) {
        String column = sort == null ? null : allowed.get(sort);
        if (column == null) {
            return defaultOrder;
        }
        return column + (descending ? " DESC" : " ASC");
    }
}
