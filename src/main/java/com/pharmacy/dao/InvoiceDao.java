package com.pharmacy.dao;

import com.pharmacy.model.Invoice;
import com.pharmacy.model.InvoiceItem;
import com.pharmacy.model.InvoiceStatus;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Payment;
import com.pharmacy.model.PaymentStatus;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InvoiceDao extends AbstractDao<Invoice> {
    private static final String SELECT = "SELECT i.id, i.invoice_no, i.patient_id, i.dispensing_id, i.subtotal, i.discount_amount, "
            + "i.tax_rate, i.tax_amount, i.grand_total, i.status, i.payment_status, i.created_by, i.created_at, i.cancelled_at, "
            + "i.cancel_reason, pt.patient_code, pt.full_name AS patient_name, pt.phone AS patient_phone, "
            + "pt.address AS patient_address, d.dispensing_no, p.id AS prescription_id, p.prescription_no, "
            + "u.full_name AS created_by_name, "
            + "(SELECT COALESCE(SUM(pay.amount), 0) FROM payments pay WHERE pay.invoice_id = i.id) AS amount_paid";
    private static final String FROM = "FROM invoices i JOIN patients pt ON pt.id = i.patient_id "
            + "JOIN dispensings d ON d.id = i.dispensing_id JOIN prescriptions p ON p.id = d.prescription_id "
            + "JOIN users u ON u.id = i.created_by";
    private static final Map<String, String> SORTS = Map.of("no", "i.invoice_no", "date", "i.created_at",
            "patient", "pt.full_name", "total", "i.grand_total", "payment", "i.payment_status");

    @Override
    protected Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice i = new Invoice();
        i.setId(rs.getLong("id"));
        i.setInvoiceNo(rs.getString("invoice_no"));
        i.setPatientId(rs.getLong("patient_id"));
        i.setPatientCode(rs.getString("patient_code"));
        i.setPatientName(rs.getString("patient_name"));
        i.setPatientPhone(rs.getString("patient_phone"));
        i.setPatientAddress(rs.getString("patient_address"));
        i.setDispensingId(rs.getLong("dispensing_id"));
        i.setDispensingNo(rs.getString("dispensing_no"));
        i.setPrescriptionId(rs.getLong("prescription_id"));
        i.setPrescriptionNo(rs.getString("prescription_no"));
        i.setSubtotal(rs.getBigDecimal("subtotal"));
        i.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        i.setTaxRate(rs.getBigDecimal("tax_rate"));
        i.setTaxAmount(rs.getBigDecimal("tax_amount"));
        i.setGrandTotal(rs.getBigDecimal("grand_total"));
        i.setStatus(InvoiceStatus.valueOf(rs.getString("status")));
        i.setPaymentStatus(PaymentStatus.valueOf(rs.getString("payment_status")));
        i.setCreatedBy(rs.getLong("created_by"));
        i.setCreatedByName(rs.getString("created_by_name"));
        i.setCreatedAt(dateTime(rs, "created_at"));
        i.setCancelledAt(dateTime(rs, "cancelled_at"));
        i.setCancelReason(rs.getString("cancel_reason"));
        i.setAmountPaid(rs.getBigDecimal("amount_paid"));
        return i;
    }

    private InvoiceItem mapItem(ResultSet rs) throws SQLException {
        InvoiceItem it = new InvoiceItem();
        it.setId(rs.getLong("id"));
        it.setInvoiceId(rs.getLong("invoice_id"));
        it.setDispensingItemId(rs.getLong("dispensing_item_id"));
        it.setMedicineId(rs.getLong("medicine_id"));
        it.setMedicineName(Medicine.buildLabel(rs.getString("name"), rs.getString("strength"), rs.getString("dosage_form")));
        it.setBatchId(rs.getLong("batch_id"));
        it.setBatchNumber(rs.getString("batch_number"));
        it.setQuantity(rs.getInt("quantity"));
        it.setUnitPrice(rs.getBigDecimal("unit_price"));
        it.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        it.setLineTotal(rs.getBigDecimal("line_total"));
        return it;
    }

    private Payment mapPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setId(rs.getLong("id"));
        p.setInvoiceId(rs.getLong("invoice_id"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setMethod(rs.getString("method"));
        p.setReference(rs.getString("reference"));
        p.setReceivedBy(rs.getLong("received_by"));
        p.setReceivedByName(rs.getString("received_by_name"));
        p.setPaidAt(dateTime(rs, "paid_at"));
        return p;
    }

    public long insert(Connection con, Invoice i) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO invoices (invoice_no, patient_id, dispensing_id, subtotal, discount_amount, tax_rate, tax_amount, "
                        + "grand_total, status, payment_status, created_by, created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                i.getInvoiceNo(), i.getPatientId(), i.getDispensingId(), i.getSubtotal(), i.getDiscountAmount(), i.getTaxRate(),
                i.getTaxAmount(), i.getGrandTotal(), i.getStatus(), i.getPaymentStatus(), i.getCreatedBy(), i.getCreatedAt());
    }

    public long insertItem(Connection con, long invoiceId, InvoiceItem it) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO invoice_items (invoice_id, dispensing_item_id, medicine_id, batch_id, quantity, unit_price, "
                        + "discount_amount, line_total) VALUES (?,?,?,?,?,?,?,?)",
                invoiceId, it.getDispensingItemId(), it.getMedicineId(), it.getBatchId(), it.getQuantity(), it.getUnitPrice(),
                it.getDiscountAmount(), it.getLineTotal());
    }

    public long insertPayment(Connection con, Payment p) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO payments (invoice_id, amount, method, reference, received_by, paid_at) VALUES (?,?,?,?,?,?)",
                p.getInvoiceId(), p.getAmount(), p.getMethod(), p.getReference(), p.getReceivedBy(), p.getPaidAt());
    }

    /** Header only. */
    public Optional<Invoice> findHeaderById(Connection con, long id, boolean forUpdate) throws SQLException {
        if (forUpdate) {
            // lock the invoice row first, then read it together with its joined data
            query(con, "SELECT id FROM invoices WHERE id = ? FOR UPDATE", rs -> rs.getLong(1), id);
        }
        return queryOne(con, SELECT + " " + FROM + " WHERE i.id = ?", id);
    }

    public Optional<Invoice> findById(Connection con, long id) throws SQLException {
        Optional<Invoice> found = findHeaderById(con, id, false);
        if (found.isPresent()) {
            Invoice inv = found.get();
            inv.setItems(query(con, "SELECT ii.id, ii.invoice_id, ii.dispensing_item_id, ii.medicine_id, ii.batch_id, ii.quantity, "
                    + "ii.unit_price, ii.discount_amount, ii.line_total, m.name, m.strength, m.dosage_form, b.batch_number "
                    + "FROM invoice_items ii JOIN medicines m ON m.id = ii.medicine_id JOIN medicine_batches b ON b.id = ii.batch_id "
                    + "WHERE ii.invoice_id = ? ORDER BY ii.id", this::mapItem, id));
            inv.setPayments(query(con, "SELECT pay.id, pay.invoice_id, pay.amount, pay.method, pay.reference, pay.received_by, "
                    + "pay.paid_at, u.full_name AS received_by_name FROM payments pay JOIN users u ON u.id = pay.received_by "
                    + "WHERE pay.invoice_id = ? ORDER BY pay.paid_at, pay.id", this::mapPayment, id));
        }
        return found;
    }

    public Optional<Long> findActiveIdByDispensing(Connection con, long dispensingId) throws SQLException {
        List<Long> ids = query(con, "SELECT id FROM invoices WHERE active_dispensing_id = ?", rs -> rs.getLong(1), dispensingId);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    public BigDecimal sumPayments(Connection con, long invoiceId) throws SQLException {
        return query(con, "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE invoice_id = ?", rs -> rs.getBigDecimal(1), invoiceId).get(0);
    }

    public void updatePaymentStatus(Connection con, long invoiceId, PaymentStatus status) throws SQLException {
        update(con, "UPDATE invoices SET payment_status = ? WHERE id = ?", status, invoiceId);
    }

    public void cancel(Connection con, long invoiceId, long userId, LocalDateTime when, String reason) throws SQLException {
        update(con, "UPDATE invoices SET status = 'CANCELLED', cancelled_by = ?, cancelled_at = ?, cancel_reason = ? WHERE id = ?",
                userId, when, reason, invoiceId);
    }

    public Page<Invoice> search(Connection con, PageRequest request, PaymentStatus paymentStatus, InvoiceStatus status,
                                LocalDate from, LocalDate to, Long patientId) throws SQLException {
        Where where = new Where().search(request.getQuery(), "i.invoice_no", "pt.full_name", "pt.patient_code", "p.prescription_no")
                .andIf(paymentStatus != null, "i.payment_status = ? AND i.status = 'ISSUED'", paymentStatus)
                .andIf(status != null, "i.status = ?", status)
                .andIf(from != null, "i.created_at >= ?", from == null ? null : from.atStartOfDay())
                .andIf(to != null, "i.created_at < ?", to == null ? null : to.plusDays(1).atStartOfDay())
                .andIf(patientId != null, "i.patient_id = ?", patientId);
        return page(con, SELECT, FROM, where, request.orderBy(SORTS, "i.created_at DESC, i.id DESC"), request);
    }

    public List<Invoice> findRecent(Connection con, int limit) throws SQLException {
        return query(con, SELECT + " " + FROM + " ORDER BY i.created_at DESC, i.id DESC LIMIT ?", limit);
    }
}
