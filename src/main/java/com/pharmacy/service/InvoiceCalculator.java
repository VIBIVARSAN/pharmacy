package com.pharmacy.service;

import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.InvoiceItem;
import com.pharmacy.util.Money;

import java.math.BigDecimal;

/**
 * Pure money arithmetic for an invoice (no database access, so it is easy to unit test).
 *
 * <pre>
 *   line gross  = quantity x unit price
 *   line total  = line gross - line discount
 *   subtotal    = sum of line gross
 *   discount    = sum of line discounts
 *   tax         = (subtotal - discount) x tax rate %      (rounded HALF_UP to 2 decimals)
 *   grand total = subtotal - discount + tax
 * </pre>
 */
public class InvoiceCalculator {

    public void calculate(Invoice invoice, BigDecimal taxRatePercent) {
        if (taxRatePercent == null || taxRatePercent.signum() < 0) {
            throw new IllegalArgumentException("A non-negative tax rate is required.");
        }
        BigDecimal subtotal = Money.ZERO;
        BigDecimal discount = Money.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            BigDecimal gross = Money.multiply(item.getUnitPrice(), item.getQuantity());
            BigDecimal lineDiscount = Money.scale(item.getDiscountAmount());
            if (lineDiscount.signum() < 0) {
                throw new ValidationException("discount_" + item.getDispensingItemId(), "A discount cannot be negative.");
            }
            if (lineDiscount.compareTo(gross) > 0) {
                throw new ValidationException("discount_" + item.getDispensingItemId(),
                        "The discount on " + item.getMedicineName() + " is larger than the line amount.");
            }
            item.setDiscountAmount(lineDiscount);
            item.setLineTotal(gross.subtract(lineDiscount));
            subtotal = subtotal.add(gross);
            discount = discount.add(lineDiscount);
        }
        BigDecimal taxable = subtotal.subtract(discount);
        BigDecimal tax = Money.percentOf(taxable, taxRatePercent);
        invoice.setSubtotal(subtotal);
        invoice.setDiscountAmount(discount);
        invoice.setTaxRate(taxRatePercent.setScale(2, java.math.RoundingMode.HALF_UP));
        invoice.setTaxAmount(tax);
        invoice.setGrandTotal(taxable.add(tax));
    }
}
