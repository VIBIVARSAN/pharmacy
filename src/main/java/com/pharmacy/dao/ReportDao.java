package com.pharmacy.dao;

import com.pharmacy.model.Medicine;
import com.pharmacy.model.ReportResult;
import com.pharmacy.model.StockTransactionType;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.ViewFunctions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Read-only reporting queries. Each method returns a {@link ReportResult}
 * whose totals are computed by SQL over the whole filtered data set.
 */
public class ReportDao extends AbstractDao<List<String>> {
    private static final Map<String, String> SALES_GROUPS = Map.of(
            "day", "DATE_FORMAT(i.created_at, '%Y-%m-%d')",
            "week", "DATE_FORMAT(i.created_at, '%x-W%v')",
            "month", "DATE_FORMAT(i.created_at, '%Y-%m')");

    @Override
    protected List<String> mapRow(ResultSet rs) throws SQLException {
        int n = rs.getMetaData().getColumnCount();
        List<String> row = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            row.add(rs.getString(i));
        }
        return row;
    }

    private static String money(ResultSet rs, String col) throws SQLException {
        return ViewFunctions.money(rs.getBigDecimal(col));
    }

    /** Pages over an arbitrary (possibly grouped) SELECT by wrapping it for the count. */
    private Page<List<String>> pageOf(Connection con, String body, List<Object> params, String orderBy,
                                      PageRequest request, RowMapper<List<String>> mapper) throws SQLException {
        long total = queryLong(con, "SELECT COUNT(*) FROM (" + body + ") counted", params.toArray());
        List<Object> all = new ArrayList<>(params);
        all.add(request.getSize());
        all.add(request.getOffset());
        List<List<String>> rows = query(con, body + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?", mapper, all.toArray());
        return new Page<>(rows, request.getPage(), request.getSize(), total);
    }

    /** Runs a single-row totals query and hands the row to the consumer. */
    private void totals(Connection con, String sql, List<Object> params, RowConsumer consumer) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params.toArray());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    consumer.accept(rs);
                }
            }
        }
    }

    @FunctionalInterface
    private interface RowConsumer {
        void accept(ResultSet rs) throws SQLException;
    }

    private static Where dateRange(Where where, String column, LocalDate from, LocalDate to) {
        return where.andIf(from != null, column + " >= ?", from == null ? null : from.atStartOfDay())
                .andIf(to != null, column + " < ?", to == null ? null : to.plusDays(1).atStartOfDay());
    }

    // ------------------------------------------------------------------ sales

    public ReportResult sales(Connection con, LocalDate from, LocalDate to, String groupBy, PageRequest request) throws SQLException {
        String group = SALES_GROUPS.getOrDefault(groupBy, SALES_GROUPS.get("day"));
        Where where = dateRange(new Where().and("i.status = 'ISSUED'"), "i.created_at", from, to);
        String body = "SELECT " + group + " AS period, COUNT(*) AS invoices, SUM(i.subtotal) AS subtotal, "
                + "SUM(i.discount_amount) AS discount, SUM(i.tax_amount) AS tax, SUM(i.grand_total) AS total "
                + "FROM invoices i" + where.sql() + " GROUP BY period";
        ReportResult r = new ReportResult().text("Period").number("Invoices").number("Subtotal").number("Discount")
                .number("Tax").number("Total");
        r.setPage(pageOf(con, body, where.params(), "period DESC", request, rs -> List.of(rs.getString("period"),
                rs.getString("invoices"), money(rs, "subtotal"), money(rs, "discount"), money(rs, "tax"), money(rs, "total"))));
        totals(con, "SELECT COUNT(*) AS invoices, COALESCE(SUM(i.subtotal),0) AS subtotal, COALESCE(SUM(i.discount_amount),0) AS discount, "
                + "COALESCE(SUM(i.tax_amount),0) AS tax, COALESCE(SUM(i.grand_total),0) AS total FROM invoices i" + where.sql(),
                where.params(), rs -> r.total("Invoices", rs.getString("invoices")).total("Subtotal", money(rs, "subtotal"))
                        .total("Discount", money(rs, "discount")).total("Tax", money(rs, "tax"))
                        .total("Total sales", money(rs, "total")));
        return r;
    }

    // ---------------------------------------------------------- prescriptions

    public ReportResult prescriptions(Connection con, LocalDate from, LocalDate to, String status, PageRequest request) throws SQLException {
        Where where = new Where().andIf(from != null, "p.prescription_date >= ?", from)
                .andIf(to != null, "p.prescription_date <= ?", to)
                .andIf(status != null && !status.isBlank(), "p.status = ?", status);
        String body = "SELECT p.prescription_no, p.prescription_date, pt.patient_code, pt.full_name AS patient, d.full_name AS doctor, "
                + "p.status, (SELECT COUNT(*) FROM prescription_items pi WHERE pi.prescription_id = p.id) AS items "
                + "FROM prescriptions p JOIN patients pt ON pt.id = p.patient_id JOIN doctors d ON d.id = p.doctor_id" + where.sql();
        ReportResult r = new ReportResult().text("Prescription").text("Date").text("Patient ID").text("Patient")
                .text("Doctor").text("Status").number("Items");
        r.setPage(pageOf(con, body, where.params(), "prescription_date DESC, prescription_no DESC", request,
                rs -> List.of(rs.getString("prescription_no"), ViewFunctions.date(date(rs, "prescription_date")),
                        rs.getString("patient_code"), rs.getString("patient"), rs.getString("doctor"),
                        ViewFunctions.label(rs.getString("status")), rs.getString("items"))));
        totals(con, "SELECT COUNT(*) AS total, COALESCE(SUM(p.status = 'COMPLETED'),0) AS completed, "
                + "COALESCE(SUM(p.status IN ('ISSUED','PARTIALLY_DISPENSED')),0) AS open, COALESCE(SUM(p.status = 'CANCELLED'),0) AS cancelled "
                + "FROM prescriptions p" + where.sql(), where.params(),
                rs -> r.total("Prescriptions", rs.getString("total")).total("Awaiting dispensing", rs.getString("open"))
                        .total("Completed", rs.getString("completed")).total("Cancelled", rs.getString("cancelled")));
        return r;
    }

    // -------------------------------------------------------------- inventory

    private static final String STOCK_BODY = "SELECT m.medicine_code, m.name, m.strength, m.dosage_form, m.category, m.unit, "
            + "m.reorder_level, COALESCE(SUM(CASE WHEN b.expiry_date >= ? THEN b.quantity ELSE 0 END), 0) AS available, "
            + "COALESCE(SUM(CASE WHEN b.expiry_date < ? THEN b.quantity ELSE 0 END), 0) AS expired, "
            + "COALESCE(SUM(CASE WHEN b.expiry_date >= ? THEN b.quantity * b.purchase_price ELSE 0 END), 0) AS stock_value "
            + "FROM medicines m LEFT JOIN medicine_batches b ON b.medicine_id = m.id WHERE m.active = 1 "
            + "GROUP BY m.id, m.medicine_code, m.name, m.strength, m.dosage_form, m.category, m.unit, m.reorder_level";

    public ReportResult inventory(Connection con, LocalDate today, PageRequest request) throws SQLException {
        List<Object> params = List.of(today, today, today);
        ReportResult r = new ReportResult().text("Code").text("Medicine").text("Category").text("Unit").number("Usable stock")
                .number("Expired stock").number("Reorder level").number("Stock value (cost)");
        r.setPage(pageOf(con, STOCK_BODY, params, "name", request, rs -> List.of(rs.getString("medicine_code"),
                Medicine.buildLabel(rs.getString("name"), rs.getString("strength"), rs.getString("dosage_form")),
                rs.getString("category"), rs.getString("unit"), rs.getString("available"), rs.getString("expired"),
                rs.getString("reorder_level"), money(rs, "stock_value"))));
        totals(con, "SELECT COUNT(*) AS medicines, COALESCE(SUM(available),0) AS units, COALESCE(SUM(expired),0) AS expired, "
                + "COALESCE(SUM(stock_value),0) AS value FROM (" + STOCK_BODY + ") s", params,
                rs -> r.total("Active medicines", rs.getString("medicines")).total("Usable units", rs.getString("units"))
                        .total("Expired units", rs.getString("expired")).total("Stock value (cost)", money(rs, "value")));
        return r;
    }

    public ReportResult lowStock(Connection con, LocalDate today, PageRequest request) throws SQLException {
        List<Object> params = List.of(today, today, today);
        String body = "SELECT * FROM (" + STOCK_BODY + ") s WHERE s.available <= s.reorder_level";
        ReportResult r = new ReportResult().text("Code").text("Medicine").text("Category").number("Usable stock")
                .number("Reorder level").number("Shortfall");
        r.setPage(pageOf(con, body, params, "available ASC, name", request, rs -> List.of(rs.getString("medicine_code"),
                Medicine.buildLabel(rs.getString("name"), rs.getString("strength"), rs.getString("dosage_form")),
                rs.getString("category"), rs.getString("available"), rs.getString("reorder_level"),
                String.valueOf(Math.max(0, rs.getInt("reorder_level") - rs.getInt("available"))))));
        totals(con, "SELECT COUNT(*) AS medicines, COALESCE(SUM(s.available = 0),0) AS out_of_stock FROM (" + STOCK_BODY
                        + ") s WHERE s.available <= s.reorder_level", params,
                rs -> r.total("Medicines at or below reorder level", rs.getString("medicines"))
                        .total("Out of stock", rs.getString("out_of_stock")));
        return r;
    }

    /** @param type "expired" or "near" */
    public ReportResult expiry(Connection con, LocalDate today, int nearDays, String type, PageRequest request) throws SQLException {
        Where where = new Where().and("b.quantity > 0");
        if ("expired".equals(type)) {
            where.and("b.expiry_date < ?", today);
        } else if ("near".equals(type)) {
            where.and("b.expiry_date >= ? AND b.expiry_date <= ?", today, today.plusDays(nearDays));
        } else {
            where.and("b.expiry_date <= ?", today.plusDays(nearDays));
        }
        String from = " FROM medicine_batches b JOIN medicines m ON m.id = b.medicine_id LEFT JOIN suppliers s ON s.id = b.supplier_id";
        String body = "SELECT m.name, m.strength, m.dosage_form, b.batch_number, b.expiry_date, b.quantity, "
                + "b.quantity * b.purchase_price AS value, s.name AS supplier" + from + where.sql();
        ReportResult r = new ReportResult().text("Medicine").text("Batch").text("Expiry date").text("Status")
                .number("Quantity").number("Value (cost)").text("Supplier");
        r.setPage(pageOf(con, body, where.params(), "expiry_date ASC", request, rs -> {
            LocalDate expiry = date(rs, "expiry_date");
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, expiry);
            String status = days < 0 ? "Expired " + (-days) + " day(s) ago" : "Expires in " + days + " day(s)";
            String supplier = rs.getString("supplier");
            return List.of(Medicine.buildLabel(rs.getString("name"), rs.getString("strength"), rs.getString("dosage_form")),
                    rs.getString("batch_number"), ViewFunctions.date(expiry), status, rs.getString("quantity"),
                    money(rs, "value"), supplier == null ? "-" : supplier);
        }));
        totals(con, "SELECT COUNT(*) AS batches, COALESCE(SUM(b.quantity),0) AS units, COALESCE(SUM(b.quantity * b.purchase_price),0) AS value"
                        + from + where.sql(), where.params(),
                rs -> r.total("Batches", rs.getString("batches")).total("Units", rs.getString("units"))
                        .total("Value (cost)", money(rs, "value")));
        return r;
    }

    // -------------------------------------------------------- stock movements

    public ReportResult stockMovements(Connection con, LocalDate from, LocalDate to, String type, PageRequest request) throws SQLException {
        Where where = dateRange(new Where(), "t.created_at", from, to);
        if (type != null && !type.isBlank()) {
            where.and("t.txn_type = ?", StockTransactionType.valueOf(type));
        }
        String from1 = " FROM stock_transactions t JOIN medicines m ON m.id = t.medicine_id "
                + "JOIN medicine_batches b ON b.id = t.batch_id JOIN users u ON u.id = t.user_id";
        String body = "SELECT t.id, t.created_at, m.name, m.strength, m.dosage_form, b.batch_number, t.txn_type, t.quantity, "
                + "t.balance_after, t.reason, u.username" + from1 + where.sql();
        ReportResult r = new ReportResult().text("Date").text("Medicine").text("Batch").text("Type").number("Quantity")
                .number("Batch balance").text("Reason").text("User");
        r.setPage(pageOf(con, body, where.params(), "created_at DESC, id DESC", request, rs -> {
            StockTransactionType t = StockTransactionType.valueOf(rs.getString("txn_type"));
            String reason = rs.getString("reason");
            return List.of(ViewFunctions.dateTime(dateTime(rs, "created_at")),
                    Medicine.buildLabel(rs.getString("name"), rs.getString("strength"), rs.getString("dosage_form")),
                    rs.getString("batch_number"), t.getLabel(), (t.isInbound() ? "+" : "-") + rs.getString("quantity"),
                    rs.getString("balance_after"), reason == null ? "" : reason, rs.getString("username"));
        }));
        totals(con, "SELECT COUNT(*) AS movements, "
                + "COALESCE(SUM(CASE WHEN t.txn_type IN ('STOCK_IN','ADJUSTMENT_IN') THEN t.quantity ELSE 0 END),0) AS units_in, "
                + "COALESCE(SUM(CASE WHEN t.txn_type IN ('STOCK_OUT','ADJUSTMENT_OUT','DISPENSE') THEN t.quantity ELSE 0 END),0) AS units_out"
                + from1 + where.sql(), where.params(),
                rs -> r.total("Movements", rs.getString("movements")).total("Units in", rs.getString("units_in"))
                        .total("Units out", rs.getString("units_out")));
        return r;
    }

    public ReportResult supplierReceipts(Connection con, LocalDate from, LocalDate to, Long supplierId, PageRequest request) throws SQLException {
        Where where = dateRange(new Where().and("t.txn_type = 'STOCK_IN'"), "t.created_at", from, to)
                .andIf(supplierId != null, "b.supplier_id = ?", supplierId);
        String from1 = " FROM stock_transactions t JOIN medicine_batches b ON b.id = t.batch_id "
                + "JOIN medicines m ON m.id = t.medicine_id LEFT JOIN suppliers s ON s.id = b.supplier_id";
        String body = "SELECT t.id, t.created_at, s.name AS supplier, m.name, m.strength, m.dosage_form, b.batch_number, t.quantity, "
                + "b.purchase_price, t.quantity * b.purchase_price AS value" + from1 + where.sql();
        ReportResult r = new ReportResult().text("Received").text("Supplier").text("Medicine").text("Batch")
                .number("Quantity").number("Unit cost").number("Value");
        r.setPage(pageOf(con, body, where.params(), "created_at DESC, id DESC", request, rs -> {
            String supplier = rs.getString("supplier");
            return List.of(ViewFunctions.dateTime(dateTime(rs, "created_at")), supplier == null ? "(not recorded)" : supplier,
                    Medicine.buildLabel(rs.getString("name"), rs.getString("strength"), rs.getString("dosage_form")),
                    rs.getString("batch_number"), rs.getString("quantity"), money(rs, "purchase_price"), money(rs, "value"));
        }));
        totals(con, "SELECT COUNT(*) AS receipts, COALESCE(SUM(t.quantity),0) AS units, "
                        + "COALESCE(SUM(t.quantity * b.purchase_price),0) AS value" + from1 + where.sql(), where.params(),
                rs -> r.total("Receipts", rs.getString("receipts")).total("Units received", rs.getString("units"))
                        .total("Purchase value", money(rs, "value")));
        return r;
    }

    // --------------------------------------------------------------- payments

    public ReportResult billing(Connection con, LocalDate from, LocalDate to, String paymentStatus, PageRequest request) throws SQLException {
        Where where = dateRange(new Where(), "i.created_at", from, to);
        if ("CANCELLED".equals(paymentStatus)) {
            where.and("i.status = 'CANCELLED'");
        } else if (paymentStatus != null && !paymentStatus.isBlank()) {
            where.and("i.status = 'ISSUED' AND i.payment_status = ?", paymentStatus);
        }
        String paid = "(SELECT COALESCE(SUM(pay.amount), 0) FROM payments pay WHERE pay.invoice_id = i.id)";
        String from1 = " FROM invoices i JOIN patients pt ON pt.id = i.patient_id";
        String body = "SELECT i.id, i.invoice_no, i.created_at, pt.patient_code, i.grand_total, " + paid + " AS paid, "
                + "i.payment_status, i.status" + from1 + where.sql();
        ReportResult r = new ReportResult().text("Invoice").text("Date").text("Patient ID").number("Total")
                .number("Paid").number("Balance").text("Payment status");
        r.setPage(pageOf(con, body, where.params(), "created_at DESC, id DESC", request, rs -> {
            boolean cancelled = "CANCELLED".equals(rs.getString("status"));
            java.math.BigDecimal balance = rs.getBigDecimal("grand_total").subtract(rs.getBigDecimal("paid")).max(java.math.BigDecimal.ZERO);
            return List.of(rs.getString("invoice_no"), ViewFunctions.dateTime(dateTime(rs, "created_at")),
                    rs.getString("patient_code"), money(rs, "grand_total"), money(rs, "paid"),
                    cancelled ? "-" : ViewFunctions.money(balance),
                    cancelled ? "Cancelled" : ViewFunctions.label(rs.getString("payment_status")));
        }));
        totals(con, "SELECT COALESCE(SUM(i.status = 'ISSUED'),0) AS invoices, "
                + "COALESCE(SUM(CASE WHEN i.status = 'ISSUED' THEN i.grand_total ELSE 0 END),0) AS billed, "
                + "COALESCE(SUM(CASE WHEN i.status = 'ISSUED' THEN " + paid + " ELSE 0 END),0) AS paid, "
                + "COALESCE(SUM(i.status = 'CANCELLED'),0) AS cancelled" + from1 + where.sql(), where.params(),
                rs -> r.total("Invoices", rs.getString("invoices")).total("Billed", money(rs, "billed"))
                        .total("Received", money(rs, "paid"))
                        .total("Outstanding", ViewFunctions.money(rs.getBigDecimal("billed").subtract(rs.getBigDecimal("paid"))))
                        .total("Cancelled invoices", rs.getString("cancelled")));
        return r;
    }
}
