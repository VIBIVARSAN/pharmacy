package com.pharmacy.exception;

/** Not enough eligible (non-expired) stock to satisfy a request. */
public class InsufficientStockException extends BusinessRuleException {
    private static final long serialVersionUID = 1L;
    private final int requested;
    private final int available;

    public InsufficientStockException(String medicineName, int requested, int available) {
        super("Insufficient stock for " + medicineName + ": requested " + requested
                + ", available " + available + ".");
        this.requested = requested;
        this.available = available;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
