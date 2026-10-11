package com.pharmacy.model;

/** Kinds of stock movement. Each constant knows whether it adds to or removes from stock. */
public enum StockTransactionType {
    STOCK_IN("Stock in", true),
    STOCK_OUT("Stock out", false),
    ADJUSTMENT_IN("Adjustment (+)", true),
    ADJUSTMENT_OUT("Adjustment (-)", false),
    DISPENSE("Dispensed", false);

    private final String label;
    private final boolean inbound;

    StockTransactionType(String label, boolean inbound) {
        this.label = label;
        this.inbound = inbound;
    }

    public String getLabel() {
        return label;
    }

    public boolean isInbound() {
        return inbound;
    }

    /** Applies this movement to a balance. */
    public int apply(int balance, int quantity) {
        return inbound ? balance + quantity : balance - quantity;
    }
}
