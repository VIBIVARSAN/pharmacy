package com.pharmacy.model;

import com.pharmacy.util.Page;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A report ready for display: column headings, one page of pre-formatted rows
 * and a set of totals that were calculated by the database over ALL matching
 * rows (not just the visible page).
 */
public class ReportResult {

    /** A column heading and whether its values are numeric (right-aligned). */
    public static class Column {
        private final String label;
        private final boolean numeric;

        public Column(String label, boolean numeric) {
            this.label = label;
            this.numeric = numeric;
        }

        public String getLabel() {
            return label;
        }

        public boolean isNumeric() {
            return numeric;
        }
    }

    private final List<Column> columns = new ArrayList<>();
    private final Map<String, String> totals = new LinkedHashMap<>();
    private Page<List<String>> page;

    public ReportResult text(String label) {
        columns.add(new Column(label, false));
        return this;
    }

    public ReportResult number(String label) {
        columns.add(new Column(label, true));
        return this;
    }

    public ReportResult total(String label, String value) {
        totals.put(label, value);
        return this;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public Map<String, String> getTotals() {
        return totals;
    }

    public Page<List<String>> getPage() {
        return page;
    }

    public void setPage(Page<List<String>> page) {
        this.page = page;
    }
}
