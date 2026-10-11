package com.pharmacy.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Helpers for currency arithmetic. All amounts are BigDecimal with 2 decimal places, rounded HALF_UP. */
public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Money() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal multiply(BigDecimal unitPrice, int quantity) {
        return scale(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    public static BigDecimal percentOf(BigDecimal amount, BigDecimal ratePercent) {
        return scale(amount.multiply(ratePercent).divide(HUNDRED, 4, RoundingMode.HALF_UP));
    }
}
