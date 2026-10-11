package com.pharmacy.dao;

import com.pharmacy.model.Dispensing;
import com.pharmacy.model.DispensingItem;
import com.pharmacy.model.Medicine;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DispensingDao extends AbstractDao<Dispensing> {
    private static final String SELECT = "SELECT d.id, d.dispensing_no, d.prescription_id, d.dispensed_by, d.dispensed_at, "
            + "d.request_token, d.notes, p.prescription_no, pt.id AS patient_id, pt.patient_code, pt.full_name AS patient_name, "
            + "u.full_name AS dispensed_by_name, i.id AS invoice_id, i.invoice_no, "
            + "(SELECT COALESCE(SUM(di.quantity * di.unit_price), 0) FROM dispensing_items di WHERE di.dispensing_id = d.id) AS total_amount";
    private static final String FROM = "FROM dispensings d JOIN prescriptions p ON p.id = d.prescription_id "
            + "JOIN patients pt ON pt.id = p.patient_id JOIN users u ON u.id = d.dispensed_by "
            + "LEFT JOIN invoices i ON i.active_dispensing_id = d.id";
    private static final Map<String, String> SORTS = Map.of("no", "d.dispensing_no", "date", "d.dispensed_at",
            "patient", "pt.full_name", "prescription", "p.prescription_no");

    @Override
    protected Dispensing mapRow(ResultSet rs) throws SQLException {
        Dispensing d = new Dispensing();
        d.setId(rs.getLong("id"));
        d.setDispensingNo(rs.getString("dispensing_no"));
        d.setPrescriptionId(rs.getLong("prescription_id"));
        d.setPrescriptionNo(rs.getString("prescription_no"));
        d.setPatientId(rs.getLong("patient_id"));
        d.setPatientCode(rs.getString("patient_code"));
        d.setPatientName(rs.getString("patient_name"));
        d.setDispensedBy(rs.getLong("dispensed_by"));
        d.setDispensedByName(rs.getString("dispensed_by_name"));
        d.setDispensedAt(dateTime(rs, "dispensed_at"));
        d.setCreatedAt(d.getDispensedAt());
        d.setRequestToken(rs.getString("request_token"));
        d.setNotes(rs.getString("notes"));
        d.setInvoiceId(longOrNull(rs, "invoice_id"));
        d.setInvoiceNo(rs.getString("invoice_no"));
        d.setTotal(rs.getBigDecimal("total_amount"));
        return d;
    }

    private DispensingItem mapItem(ResultSet rs) throws SQLException {
        DispensingItem i = new DispensingItem();
        i.setId(rs.getLong("id"));
        i.setDispensingId(rs.getLong("dispensing_id"));
        i.setPrescriptionItemId(rs.getLong("prescription_item_id"));
        i.setMedicineId(rs.getLong("medicine_id"));
        i.setMedicineName(Medicine.buildLabel(rs.getString("name"), rs.getString("strength"), rs.getString("dosage_form")));
        i.setBatchId(rs.getLong("batch_id"));
        i.setBatchNumber(rs.getString("batch_number"));
        i.setExpiryDate(date(rs, "expiry_date"));
        i.setQuantity(rs.getInt("quantity"));
        i.setUnitPrice(rs.getBigDecimal("unit_price"));
        return i;
    }

    public long insert(Connection con, Dispensing d) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO dispensings (dispensing_no, prescription_id, dispensed_by, dispensed_at, request_token, notes) "
                        + "VALUES (?,?,?,?,?,?)",
                d.getDispensingNo(), d.getPrescriptionId(), d.getDispensedBy(), d.getDispensedAt(), d.getRequestToken(), d.getNotes());
    }

    public long insertItem(Connection con, long dispensingId, DispensingItem i) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO dispensing_items (dispensing_id, prescription_item_id, medicine_id, batch_id, quantity, unit_price) "
                        + "VALUES (?,?,?,?,?,?)",
                dispensingId, i.getPrescriptionItemId(), i.getMedicineId(), i.getBatchId(), i.getQuantity(), i.getUnitPrice());
    }

    public Optional<Dispensing> findById(Connection con, long id) throws SQLException {
        Optional<Dispensing> found = queryOne(con, SELECT + " " + FROM + " WHERE d.id = ?", id);
        if (found.isPresent()) {
            found.get().setItems(findItems(con, id));
        }
        return found;
    }

    public List<DispensingItem> findItems(Connection con, long dispensingId) throws SQLException {
        return query(con, "SELECT di.id, di.dispensing_id, di.prescription_item_id, di.medicine_id, di.batch_id, di.quantity, "
                + "di.unit_price, m.name, m.strength, m.dosage_form, b.batch_number, b.expiry_date "
                + "FROM dispensing_items di JOIN medicines m ON m.id = di.medicine_id "
                + "JOIN medicine_batches b ON b.id = di.batch_id WHERE di.dispensing_id = ? ORDER BY di.id",
                this::mapItem, dispensingId);
    }

    public Optional<Long> findIdByToken(Connection con, String requestToken) throws SQLException {
        List<Long> ids = query(con, "SELECT id FROM dispensings WHERE request_token = ?", rs -> rs.getLong(1), requestToken);
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }

    /** Locks the dispensing row; used by billing so two invoices cannot be created for it at once. */
    public boolean lockById(Connection con, long id) throws SQLException {
        return !query(con, "SELECT id FROM dispensings WHERE id = ? FOR UPDATE", rs -> rs.getLong(1), id).isEmpty();
    }

    public List<Dispensing> findByPrescription(Connection con, long prescriptionId) throws SQLException {
        return query(con, SELECT + " " + FROM + " WHERE d.prescription_id = ? ORDER BY d.dispensed_at DESC, d.id DESC", prescriptionId);
    }

    /** @param billing "billed", "unbilled" or null */
    public Page<Dispensing> search(Connection con, PageRequest request, String billing, LocalDate from, LocalDate to)
            throws SQLException {
        Where where = new Where().search(request.getQuery(), "d.dispensing_no", "p.prescription_no", "pt.full_name", "pt.patient_code")
                .andIf("billed".equals(billing), "i.id IS NOT NULL")
                .andIf("unbilled".equals(billing), "i.id IS NULL")
                .andIf(from != null, "d.dispensed_at >= ?", from == null ? null : from.atStartOfDay())
                .andIf(to != null, "d.dispensed_at < ?", to == null ? null : to.plusDays(1).atStartOfDay());
        return page(con, SELECT, FROM, where, request.orderBy(SORTS, "d.dispensed_at DESC, d.id DESC"), request);
    }

    public List<Dispensing> findRecent(Connection con, int limit) throws SQLException {
        return query(con, SELECT + " " + FROM + " ORDER BY d.dispensed_at DESC, d.id DESC LIMIT ?", limit);
    }
}
