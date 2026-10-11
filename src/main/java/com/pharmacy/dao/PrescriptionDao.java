package com.pharmacy.dao;

import com.pharmacy.model.Doctor;
import com.pharmacy.model.Gender;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Patient;
import com.pharmacy.model.Prescription;
import com.pharmacy.model.PrescriptionItem;
import com.pharmacy.model.PrescriptionStatus;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PrescriptionDao extends AbstractDao<Prescription> {
    private static final String SELECT = "SELECT p.id, p.prescription_no, p.patient_id, p.doctor_id, p.prescription_date, "
            + "p.diagnosis, p.notes, p.status, p.created_by, p.created_at, p.updated_at, "
            + "pt.patient_code, pt.full_name AS patient_name, pt.date_of_birth, pt.gender, pt.phone AS patient_phone, "
            + "pt.address AS patient_address, pt.active AS patient_active, "
            + "d.doctor_code, d.full_name AS doctor_name, d.specialization, d.hospital, d.registration_no, "
            + "d.phone AS doctor_phone, d.active AS doctor_active, u.full_name AS created_by_name";
    private static final String FROM = "FROM prescriptions p JOIN patients pt ON pt.id = p.patient_id "
            + "JOIN doctors d ON d.id = p.doctor_id JOIN users u ON u.id = p.created_by";
    private static final Map<String, String> SORTS = Map.of("no", "p.prescription_no", "date", "p.prescription_date",
            "patient", "pt.full_name", "doctor", "d.full_name", "status", "p.status");

    @Override
    protected Prescription mapRow(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getLong("id"));
        p.setPrescriptionNo(rs.getString("prescription_no"));
        p.setPrescriptionDate(date(rs, "prescription_date"));
        p.setDiagnosis(rs.getString("diagnosis"));
        p.setNotes(rs.getString("notes"));
        p.setStatus(PrescriptionStatus.valueOf(rs.getString("status")));
        p.setCreatedBy(rs.getLong("created_by"));
        p.setCreatedByName(rs.getString("created_by_name"));
        p.setCreatedAt(dateTime(rs, "created_at"));
        p.setUpdatedAt(dateTime(rs, "updated_at"));

        Patient patient = new Patient(rs.getLong("patient_id"), rs.getString("patient_code"), rs.getString("patient_name"));
        patient.setDateOfBirth(date(rs, "date_of_birth"));
        patient.setGender(Gender.valueOf(rs.getString("gender")));
        patient.setPhone(rs.getString("patient_phone"));
        patient.setAddress(rs.getString("patient_address"));
        patient.setActive(rs.getBoolean("patient_active"));
        p.setPatient(patient);

        Doctor doctor = new Doctor(rs.getLong("doctor_id"), rs.getString("doctor_code"), rs.getString("doctor_name"));
        doctor.setSpecialization(rs.getString("specialization"));
        doctor.setHospital(rs.getString("hospital"));
        doctor.setRegistrationNo(rs.getString("registration_no"));
        doctor.setPhone(rs.getString("doctor_phone"));
        doctor.setActive(rs.getBoolean("doctor_active"));
        p.setDoctor(doctor);
        return p;
    }

    private PrescriptionItem mapItem(ResultSet rs) throws SQLException {
        PrescriptionItem i = new PrescriptionItem();
        i.setId(rs.getLong("id"));
        i.setPrescriptionId(rs.getLong("prescription_id"));
        i.setMedicineId(rs.getLong("medicine_id"));
        i.setMedicineName(Medicine.buildLabel(rs.getString("name"), rs.getString("strength"), rs.getString("dosage_form")));
        i.setDosage(rs.getString("dosage"));
        i.setFrequency(rs.getString("frequency"));
        i.setDuration(rs.getString("duration"));
        i.setInstructions(rs.getString("instructions"));
        i.setQuantity(rs.getInt("quantity"));
        i.setDispensedQuantity(rs.getInt("dispensed"));
        i.setAvailableStock(rs.getInt("available"));
        return i;
    }

    /** Header only (no items). */
    public Optional<Prescription> findHeaderById(Connection con, long id) throws SQLException {
        return queryOne(con, SELECT + " " + FROM + " WHERE p.id = ?", id);
    }

    /** Header plus items, each item carrying its dispensed quantity and currently usable stock. */
    public Optional<Prescription> findById(Connection con, long id) throws SQLException {
        Optional<Prescription> found = findHeaderById(con, id);
        if (found.isPresent()) {
            found.get().setItems(findItems(con, id, LocalDate.now()));
        }
        return found;
    }

    /** Locks the prescription row so concurrent dispensing / editing of the same prescription is serialized. */
    public Optional<PrescriptionStatus> lockAndGetStatus(Connection con, long id) throws SQLException {
        List<PrescriptionStatus> rows = query(con, "SELECT status FROM prescriptions WHERE id = ? FOR UPDATE",
                rs -> PrescriptionStatus.valueOf(rs.getString(1)), id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<PrescriptionItem> findItems(Connection con, long prescriptionId, LocalDate today) throws SQLException {
        return query(con, "SELECT pi.id, pi.prescription_id, pi.medicine_id, pi.dosage, pi.frequency, pi.duration, "
                + "pi.instructions, pi.quantity, m.name, m.strength, m.dosage_form, "
                + "COALESCE((SELECT SUM(di.quantity) FROM dispensing_items di WHERE di.prescription_item_id = pi.id), 0) AS dispensed, "
                + "COALESCE((SELECT SUM(b.quantity) FROM medicine_batches b WHERE b.medicine_id = pi.medicine_id "
                + "AND b.expiry_date >= ?), 0) AS available "
                + "FROM prescription_items pi JOIN medicines m ON m.id = pi.medicine_id "
                + "WHERE pi.prescription_id = ? ORDER BY pi.id", this::mapItem, today, prescriptionId);
    }

    public long insert(Connection con, Prescription p) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO prescriptions (prescription_no, patient_id, doctor_id, prescription_date, diagnosis, notes, status, "
                        + "created_by, created_at) VALUES (?,?,?,?,?,?,?,?,?)",
                p.getPrescriptionNo(), p.getPatientId(), p.getDoctorId(), p.getPrescriptionDate(), p.getDiagnosis(),
                p.getNotes(), p.getStatus(), p.getCreatedBy(), now());
    }

    public void update(Connection con, Prescription p) throws SQLException {
        update(con, "UPDATE prescriptions SET patient_id = ?, doctor_id = ?, prescription_date = ?, diagnosis = ?, notes = ?, "
                        + "status = ? WHERE id = ?",
                p.getPatientId(), p.getDoctorId(), p.getPrescriptionDate(), p.getDiagnosis(), p.getNotes(), p.getStatus(), p.getId());
    }

    public void updateStatus(Connection con, long id, PrescriptionStatus status) throws SQLException {
        update(con, "UPDATE prescriptions SET status = ? WHERE id = ?", status, id);
    }

    public void deleteItems(Connection con, long prescriptionId) throws SQLException {
        update(con, "DELETE FROM prescription_items WHERE prescription_id = ?", prescriptionId);
    }

    public long insertItem(Connection con, long prescriptionId, PrescriptionItem i) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO prescription_items (prescription_id, medicine_id, dosage, frequency, duration, instructions, quantity) "
                        + "VALUES (?,?,?,?,?,?,?)",
                prescriptionId, i.getMedicineId(), i.getDosage(), i.getFrequency(), i.getDuration(), i.getInstructions(), i.getQuantity());
    }

    public boolean hasDispensings(Connection con, long prescriptionId) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM dispensings WHERE prescription_id = ?", prescriptionId) > 0;
    }

    /** @param statusFilter a PrescriptionStatus name, "DISPENSABLE" (issued or partially dispensed) or null */
    public Page<Prescription> search(Connection con, PageRequest request, String statusFilter, Long patientId,
                                     Long doctorId, LocalDate from, LocalDate to) throws SQLException {
        Where where = new Where().search(request.getQuery(), "p.prescription_no", "pt.full_name", "pt.patient_code", "d.full_name");
        if ("DISPENSABLE".equals(statusFilter)) {
            where.and("p.status IN ('ISSUED','PARTIALLY_DISPENSED')");
        } else if (statusFilter != null && !statusFilter.isBlank()) {
            where.and("p.status = ?", statusFilter);
        }
        where.andIf(patientId != null, "p.patient_id = ?", patientId)
                .andIf(doctorId != null, "p.doctor_id = ?", doctorId)
                .andIf(from != null, "p.prescription_date >= ?", from)
                .andIf(to != null, "p.prescription_date <= ?", to);
        return page(con, SELECT, FROM, where, request.orderBy(SORTS, "p.created_at DESC, p.id DESC"), request);
    }

    public List<Prescription> findRecent(Connection con, int limit) throws SQLException {
        return query(con, SELECT + " " + FROM + " ORDER BY p.created_at DESC, p.id DESC LIMIT ?", limit);
    }

    public long countAll(Connection con) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM prescriptions WHERE status <> 'CANCELLED'");
    }
}
