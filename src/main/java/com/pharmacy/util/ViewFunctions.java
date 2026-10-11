package com.pharmacy.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatting functions exposed to JSP pages through WEB-INF/pms.tld (prefix "f"). */
public final class ViewFunctions {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private ViewFunctions() {
    }

    public static String date(LocalDate d) {
        return d == null ? "" : DATE.format(d);
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? "" : DATE_TIME.format(d);
    }

    public static String money(BigDecimal amount) {
        BigDecimal v = amount == null ? BigDecimal.ZERO : amount;
        DecimalFormat df = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df.format(v);
    }

    /** Turns an enum-style constant such as PARTIALLY_DISPENSED into "Partially dispensed". */
    public static String label(Object constant) {
        if (constant == null) {
            return "";
        }
        String s = constant.toString().replace('_', ' ').toLowerCase(Locale.ENGLISH);
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
