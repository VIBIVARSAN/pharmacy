package com.pharmacy.dao;

import com.pharmacy.model.Medicine;
import com.pharmacy.model.MedicineBatch;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BatchDao extends AbstractDao<MedicineBatch> {
    private static final String SELECT = "SELECT b.id, b.medicine_id, b.batch_number, b.expiry_date, b.purchase_price, "
            + "b.selling_price, b.quantity, b.supplier_id, b.received_date, b.created_at, b.updated_at, "
            + "m.name AS medicine_name, m.strength, m.dosage_form, m.medicine_code, s.name AS supplier_name";
    private static final String FROM = "FROM medicine_batches b JOIN medicines m ON m.id = b.medicine_id "
            + "LEFT JOIN suppliers s ON s.id = b.supplier_id";
    private static final Map<String, String> SORTS = Map.of("medicine", "m.name", "batch", "b.batch_number",
            "expiry", "b.expiry_date", "quantity", "b.quantity", "received", "b.received_date");

    @Override
    protected MedicineBatch mapRow(ResultSet rs) throws SQLException {
        MedicineBatch b = new MedicineBatch();
        b.setId(rs.getLong("id"));
        b.setMedicineId(rs.getLong("medicine_id"));
        b.setBatchNumber(rs.getString("batch_number"));
        b.setExpiryDate(date(rs, "expiry_date"));
        b.setPurchasePrice(rs.getBigDecimal("purchase_price"));
        b.setSellingPrice(rs.getBigDecimal("selling_price"));
        b.setQuantity(rs.getInt("quantity"));
        b.setSupplierId(longOrNull(rs, "supplier_id"));
        b.setReceivedDate(date(rs, "received_date"));
        b.setCreatedAt(dateTime(rs, "created_at"));
        b.setUpdatedAt(dateTime(rs, "updated_at"));
        b.setMedicineName(Medicine.buildLabel(rs.getString("medicine_name"), rs.getString("strength"), rs.getString("dosage_form")));
        b.setMedicineCode(rs.getString("medicine_code"));
        b.setSupplierName(rs.getString("supplier_name"));
        return b;
    }

    public Optional<MedicineBatch> findById(Connection con, long id) throws SQLException {
        return queryOne(con, SELECT + " " + FROM + " WHERE b.id = ?", id);
    }

    /** Loads a batch and locks its row until the surrounding transaction ends. */
    public Optional<MedicineBatch> findByIdForUpdate(Connection con, long id) throws SQLException {
        return queryOne(con, SELECT + " " + FROM + " WHERE b.id = ? FOR UPDATE", id);
    }

    public Optional<MedicineBatch> findByNumberForUpdate(Connection con, long medicineId, String batchNumber) throws SQLException {
        return queryOne(con, SELECT + " " + FROM + " WHERE b.medicine_id = ? AND b.batch_number = ? FOR UPDATE",
                medicineId, batchNumber);
    }

    /**
     * Locks every batch of the medicine that can still be dispensed, ordered
     * first-expiry-first-out. Expired and empty batches are never returned.
     */
    public List<MedicineBatch> lockEligibleBatches(Connection con, long medicineId, LocalDate today) throws SQLException {
        return query(con, SELECT + " " + FROM + " WHERE b.medicine_id = ? AND b.quantity > 0 AND b.expiry_date >= ? "
                + "ORDER BY b.expiry_date ASC, b.id ASC FOR UPDATE", medicineId, today);
    }

    public List<MedicineBatch> findByMedicine(Connection con, long medicineId) throws SQLException {
        return query(con, SELECT + " " + FROM + " WHERE b.medicine_id = ? ORDER BY b.expiry_date ASC, b.id ASC", medicineId);
    }

    public long insert(Connection con, MedicineBatch b) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO medicine_batches (medicine_id, batch_number, expiry_date, purchase_price, selling_price, quantity, "
                        + "supplier_id, received_date, created_at) VALUES (?,?,?,?,?,?,?,?,?)",
                b.getMedicineId(), b.getBatchNumber(), b.getExpiryDate(), b.getPurchasePrice(), b.getSellingPrice(),
                b.getQuantity(), b.getSupplierId(), b.getReceivedDate(), now());
    }

    public void increaseQuantity(Connection con, long batchId, int quantity) throws SQLException {
        update(con, "UPDATE medicine_batches SET quantity = quantity + ? WHERE id = ?", quantity, batchId);
    }

    /**
     * Deducts stock only if enough is left. The "quantity >= ?" guard makes the
     * statement itself refuse to drive stock negative.
     *
     * @return true when the deduction happened
     */
    public boolean decreaseQuantity(Connection con, long batchId, int quantity) throws SQLException {
        return update(con, "UPDATE medicine_batches SET quantity = quantity - ? WHERE id = ? AND quantity >= ?",
                quantity, batchId, quantity) == 1;
    }

    public void updatePrices(Connection con, MedicineBatch b) throws SQLException {
        update(con, "UPDATE medicine_batches SET purchase_price = ?, selling_price = ?, supplier_id = ? WHERE id = ?",
                b.getPurchasePrice(), b.getSellingPrice(), b.getSupplierId(), b.getId());
    }

    /** @param status "instock", "near", "expired", "empty" or null for everything */
    public Page<MedicineBatch> search(Connection con, PageRequest request, String status, LocalDate today,
                                      int nearExpiryDays, Long supplierId) throws SQLException {
        Where where = new Where().search(request.getQuery(), "m.name", "m.medicine_code", "b.batch_number")
                .andIf("instock".equals(status), "b.quantity > 0 AND b.expiry_date >= ?", today)
                .andIf("near".equals(status), "b.quantity > 0 AND b.expiry_date >= ? AND b.expiry_date <= ?",
                        today, today.plusDays(nearExpiryDays))
                .andIf("expired".equals(status), "b.quantity > 0 AND b.expiry_date < ?", today)
                .andIf("empty".equals(status), "b.quantity = 0")
                .andIf(supplierId != null, "b.supplier_id = ?", supplierId);
        return page(con, SELECT, FROM, where, request.orderBy(SORTS, "b.expiry_date ASC, b.id ASC"), request);
    }

    public long countNearExpiry(Connection con, LocalDate today, int days) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM medicine_batches WHERE quantity > 0 AND expiry_date >= ? AND expiry_date <= ?",
                today, today.plusDays(days));
    }

    public long countExpired(Connection con, LocalDate today) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM medicine_batches WHERE quantity > 0 AND expiry_date < ?", today);
    }
}
