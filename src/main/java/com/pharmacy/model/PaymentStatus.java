package com.pharmacy.model;

import java.math.BigDecimal;

public enum PaymentStatus {
    UNPAID("Unpaid"), PARTIALLY_PAID("Partially paid"), PAID("Paid");

    private final String label;

    PaymentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Derives the status from how much has been paid against the invoice total. */
    public static PaymentStatus of(BigDecimal paid, BigDecimal total) {
        if (paid.compareTo(total) >= 0) {
            return PAID;
        }
        return paid.signum() > 0 ? PARTIALLY_PAID : UNPAID;
    }
}
