package com.pharmacy.service;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.InvoiceItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvoiceCalculatorTest {
    private final InvoiceCalculator calculator = new InvoiceCalculator();

    private static InvoiceItem line(long id, int quantity, String unitPrice, String discount) {
        InvoiceItem item = new InvoiceItem();
        item.setDispensingItemId(id);
        item.setMedicineName("Medicine " + id);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setDiscountAmount(discount == null ? null : new BigDecimal(discount));
        return item;
    }

    @Test
    void calculatesSubtotalDiscountTaxAndGrandTotal() {
        Invoice invoice = new Invoice();
        invoice.getItems().add(line(1, 10, "2.50", "2.25"));
        invoice.getItems().add(line(2, 2, "3.00", null));
        invoice.getItems().add(line(3, 5, "7.25", "0"));
        calculator.calculate(invoice, new BigDecimal("5"));
        assertEquals(new BigDecimal("67.25"), invoice.getSubtotal());
        assertEquals(new BigDecimal("2.25"), invoice.getDiscountAmount());
        assertEquals(new BigDecimal("3.25"), invoice.getTaxAmount());
        assertEquals(new BigDecimal("68.25"), invoice.getGrandTotal());
        assertEquals(new BigDecimal("22.75"), invoice.getItems().get(0).getLineTotal());
        assertEquals(new BigDecimal("5.00"), invoice.getTaxRate());
    }

    @Test
    void roundsTaxHalfUpToTwoDecimals() {
        Invoice invoice = new Invoice();
        invoice.getItems().add(line(1, 1, "10.05", null));
        calculator.calculate(invoice, new BigDecimal("12.5"));   // 1.25625 -> 1.26
        assertEquals(new BigDecimal("1.26"), invoice.getTaxAmount());
        assertEquals(new BigDecimal("11.31"), invoice.getGrandTotal());
    }

    @Test
    void usesExactDecimalArithmetic() {
        Invoice invoice = new Invoice();
        invoice.getItems().add(line(1, 3, "0.10", null));           // 0.30 exactly, not 0.30000000000000004
        calculator.calculate(invoice, BigDecimal.ZERO);
        assertEquals(new BigDecimal("0.30"), invoice.getGrandTotal());
        assertEquals(new BigDecimal("0.00"), invoice.getTaxAmount());
    }

    @Test
    void rejectsDiscountLargerThanTheLine() {
        Invoice invoice = new Invoice();
        invoice.getItems().add(line(1, 2, "3.00", "6.01"));
        assertThrows(ValidationException.class, () -> calculator.calculate(invoice, BigDecimal.ZERO));
    }

    @Test
    void rejectsNegativeDiscount() {
        Invoice invoice = new Invoice();
        invoice.getItems().add(line(1, 2, "3.00", "-1"));
        assertThrows(ValidationException.class, () -> calculator.calculate(invoice, BigDecimal.ZERO));
    }

    @Test
    void requiresAConfiguredTaxRate() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(new Invoice(), null));
    }
}
