package com.pharmacy.dao;

import com.pharmacy.model.Medicine;
import com.pharmacy.model.StockTransaction;
import com.pharmacy.model.StockTransactionType;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** The stock ledger is append-only: this DAO has no update or delete method on purpose. */
public class StockTransactionDao extends AbstractDao<StockTransaction> {
    private static final String SELECT = "SELECT t.id, t.batch_id, t.medicine_id, t.txn_type, t.quantity, t.balance_after, "
            + "t.reason, t.reference_type, t.reference_id, t.user_id, t.created_at, m.name AS medicine_name, m.strength, "
            + "m.dosage_form, b.batch_number, u.username";
    private static final String FROM = "FROM stock_transactions t JOIN medicines m ON m.id = t.medicine_id "
            + "JOIN medicine_batches b ON b.id = t.batch_id JOIN users u ON u.id = t.user_id";
    private static final Map<String, String> SORTS = Map.of("date", "t.created_at", "medicine", "m.name",
            "type", "t.txn_type", "quantity", "t.quantity");

    @Override
    protected StockTransaction mapRow(ResultSet rs) throws SQLException {
        StockTransaction t = new StockTransaction();
        t.setId(rs.getLong("id"));
        t.setBatchId(rs.getLong("batch_id"));
        t.setMedicineId(rs.getLong("medicine_id"));
        t.setType(StockTransactionType.valueOf(rs.getString("txn_type")));
        t.setQuantity(rs.getInt("quantity"));
        t.setBalanceAfter(rs.getInt("balance_after"));
        t.setReason(rs.getString("reason"));
        t.setReferenceType(rs.getString("reference_type"));
        t.setReferenceId(longOrNull(rs, "reference_id"));
        t.setUserId(rs.getLong("user_id"));
        t.setCreatedAt(dateTime(rs, "created_at"));
        t.setMedicineName(Medicine.buildLabel(rs.getString("medicine_name"), rs.getString("strength"), rs.getString("dosage_form")));
        t.setBatchNumber(rs.getString("batch_number"));
        t.setUsername(rs.getString("username"));
        return t;
    }

    public long insert(Connection con, StockTransaction t) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO stock_transactions (batch_id, medicine_id, txn_type, quantity, balance_after, reason, "
                        + "reference_type, reference_id, user_id, created_at) VALUES (?,?,?,?,?,?,?,?,?,?)",
                t.getBatchId(), t.getMedicineId(), t.getType(), t.getQuantity(), t.getBalanceAfter(), t.getReason(),
                t.getReferenceType(), t.getReferenceId(), t.getUserId(), now());
    }

    public Page<StockTransaction> search(Connection con, PageRequest request, StockTransactionType type,
                                         LocalDate from, LocalDate to, Long medicineId) throws SQLException {
        Where where = new Where().search(request.getQuery(), "m.name", "m.medicine_code", "b.batch_number", "t.reason")
                .andIf(type != null, "t.txn_type = ?", type)
                .andIf(from != null, "t.created_at >= ?", from == null ? null : from.atStartOfDay())
                .andIf(to != null, "t.created_at < ?", to == null ? null : to.plusDays(1).atStartOfDay())
                .andIf(medicineId != null, "t.medicine_id = ?", medicineId);
        return page(con, SELECT, FROM, where, request.orderBy(SORTS, "t.created_at DESC, t.id DESC"), request);
    }

    public List<StockTransaction> findRecentForMedicine(Connection con, long medicineId, int limit) throws SQLException {
        return query(con, SELECT + " " + FROM + " WHERE t.medicine_id = ? ORDER BY t.created_at DESC, t.id DESC LIMIT ?",
                medicineId, limit);
    }
}
