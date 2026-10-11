package com.pharmacy.service;

import com.pharmacy.dao.DoctorDao;
import com.pharmacy.dao.MedicineDao;
import com.pharmacy.dao.PatientDao;
import com.pharmacy.dao.PrescriptionDao;
import com.pharmacy.dao.SequenceDao;
import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.model.Doctor;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Patient;
import com.pharmacy.model.Permission;
import com.pharmacy.model.Prescription;
import com.pharmacy.model.PrescriptionItem;
import com.pharmacy.model.PrescriptionStatus;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Prescription life-cycle: DRAFT -> ISSUED -> PARTIALLY_DISPENSED -> COMPLETED, or CANCELLED.
 * The system only records what an authorised person typed in; it never
 * suggests a diagnosis, medicine or dosage.
 */
public class PrescriptionService extends BaseService {
    private final PrescriptionDao prescriptionDao;
    private final PatientDao patientDao;
    private final DoctorDao doctorDao;
    private final MedicineDao medicineDao;
    private final SequenceDao sequenceDao;

    public PrescriptionService(TransactionManager tx, AuditService audit, PrescriptionDao prescriptionDao,
                               PatientDao patientDao, DoctorDao doctorDao, MedicineDao medicineDao, SequenceDao sequenceDao) {
        super(tx, audit);
        this.prescriptionDao = prescriptionDao;
        this.patientDao = patientDao;
        this.doctorDao = doctorDao;
        this.medicineDao = medicineDao;
        this.sequenceDao = sequenceDao;
    }

    public Page<Prescription> search(User actor, PageRequest request, String status, Long patientId, Long doctorId,
                                     LocalDate from, LocalDate to) {
        require(actor, Permission.PRESCRIPTION_VIEW);
        return tx.read(con -> prescriptionDao.search(con, request, status, patientId, doctorId, from, to));
    }

    public Prescription get(User actor, long id) {
        require(actor, Permission.PRESCRIPTION_VIEW);
        return tx.read(con -> prescriptionDao.findById(con, id)).orElseThrow(() -> new NotFoundException("Prescription"));
    }

    /**
     * Checks the header and items, and replaces the lightweight patient /
     * doctor references with the real database rows.
     */
    private void validate(Connection con, Prescription p, boolean issuing) throws SQLException {
        p.setDiagnosis(clean(p.getDiagnosis()));
        p.setNotes(clean(p.getNotes()));
        Validator v = new Validator()
                .required("patientId", p.getPatientId(), "Patient")
                .required("doctorId", p.getDoctorId(), "Doctor")
                .required("prescriptionDate", p.getPrescriptionDate(), "Prescription date")
                .notFuture("prescriptionDate", p.getPrescriptionDate(), "Prescription date")
                .maxLength("diagnosis", p.getDiagnosis(), 500, "Diagnosis")
                .maxLength("notes", p.getNotes(), 1000, "Notes");

        if (p.getPatientId() != null) {
            Optional<Patient> patient = patientDao.findById(con, p.getPatientId());
            v.check(patient.isPresent(), "patientId", "Select a registered patient.");
            v.check(patient.isEmpty() || patient.get().isActive(), "patientId", "This patient record is deactivated.");
            patient.ifPresent(p::setPatient);
        }
        if (p.getDoctorId() != null) {
            Optional<Doctor> doctor = doctorDao.findById(con, p.getDoctorId());
            v.check(doctor.isPresent(), "doctorId", "Select a registered doctor.");
            v.check(doctor.isEmpty() || doctor.get().isActive(), "doctorId", "This doctor is deactivated and cannot issue new prescriptions.");
            doctor.ifPresent(p::setDoctor);
        }

        v.check(!p.getItems().isEmpty(), "items", issuing ? "Add at least one medicine before issuing the prescription."
                : "Add at least one medicine.");
        Set<Long> seen = new HashSet<>();
        int row = 0;
        for (PrescriptionItem item : p.getItems()) {
            row++;
            String prefix = "Medicine row " + row + ": ";
            item.setDosage(clean(item.getDosage()));
            item.setFrequency(clean(item.getFrequency()));
            item.setDuration(clean(item.getDuration()));
            item.setInstructions(clean(item.getInstructions()));
            if (item.getMedicineId() == null) {
                v.reject("items", prefix + "select a medicine.");
                continue;
            }
            Optional<Medicine> medicine = medicineDao.findById(con, item.getMedicineId());
            if (medicine.isEmpty() || !medicine.get().isActive()) {
                v.reject("items", prefix + "the selected medicine is not available in the catalog.");
                continue;
            }
            item.setMedicineName(medicine.get().getLabel());
            v.check(seen.add(item.getMedicineId()), "items", prefix + medicine.get().getName() + " is listed more than once.");
            v.check(!Validator.isBlank(item.getDosage()) && item.getDosage().length() <= 100, "items",
                    prefix + "dosage is required (max 100 characters).");
            v.check(!Validator.isBlank(item.getFrequency()) && item.getFrequency().length() <= 100, "items",
                    prefix + "frequency is required (max 100 characters).");
            v.check(item.getDuration() == null || item.getDuration().length() <= 100, "items", prefix + "duration is too long.");
            v.check(item.getInstructions() == null || item.getInstructions().length() <= 300, "items", prefix + "instructions are too long.");
            v.check(item.getQuantity() != null && item.getQuantity() > 0 && item.getQuantity() <= 100000, "items",
                    prefix + "quantity must be a whole number greater than zero.");
        }
        v.throwIfInvalid();
    }

    /** Creates a prescription as a draft, or issues it straight away when {@code issue} is true. */
    public Prescription create(User actor, Prescription p, boolean issue) {
        require(actor, Permission.PRESCRIPTION_MANAGE);
        return tx.inTransaction(con -> {
            validate(con, p, issue);
            p.setPrescriptionNo(sequenceDao.nextCode(con, "PRESCRIPTION", "RX"));
            p.setStatus(issue ? PrescriptionStatus.ISSUED : PrescriptionStatus.DRAFT);
            p.setCreatedBy(actor.getId());
            p.setId(prescriptionDao.insert(con, p));
            for (PrescriptionItem item : p.getItems()) {
                item.setId(prescriptionDao.insertItem(con, p.getId(), item));
            }
            audit.log(con, actor, "PRESCRIPTION_CREATED", p, "Created prescription " + p.getPrescriptionNo()
                    + " (" + p.getStatus().getLabel() + ", " + p.getItems().size() + " item(s))");
            return p;
        });
    }

    /**
     * Edits a prescription. Only allowed while nothing has been dispensed;
     * afterwards the clinical record is frozen. The audit entry lists which
     * parts changed (never the clinical values themselves).
     */
    public void update(User actor, Prescription p, boolean issue) {
        require(actor, Permission.PRESCRIPTION_MANAGE);
        tx.inTransaction(con -> {
            PrescriptionStatus current = prescriptionDao.lockAndGetStatus(con, p.getId())
                    .orElseThrow(() -> new NotFoundException("Prescription"));
            if (!current.isEditable() || prescriptionDao.hasDispensings(con, p.getId())) {
                throw new BusinessRuleException("This prescription is " + current.getLabel().toLowerCase()
                        + " and can no longer be edited.");
            }
            Prescription before = prescriptionDao.findById(con, p.getId()).orElseThrow(() -> new NotFoundException("Prescription"));
            validate(con, p, issue || current == PrescriptionStatus.ISSUED);
            p.setPrescriptionNo(before.getPrescriptionNo());
            p.setStatus(issue ? PrescriptionStatus.ISSUED : current);
            prescriptionDao.update(con, p);
            prescriptionDao.deleteItems(con, p.getId());
            for (PrescriptionItem item : p.getItems()) {
                item.setId(prescriptionDao.insertItem(con, p.getId(), item));
            }
            audit.log(con, actor, "PRESCRIPTION_UPDATED", p, "Updated prescription " + before.getPrescriptionNo()
                    + ": " + describeChanges(before, p));
            return null;
        });
    }

    private static String describeChanges(Prescription before, Prescription after) {
        List<String> changed = new ArrayList<>();
        if (!Objects.equals(before.getPatientId(), after.getPatientId())) {
            changed.add("patient");
        }
        if (!Objects.equals(before.getDoctorId(), after.getDoctorId())) {
            changed.add("doctor");
        }
        if (!Objects.equals(before.getPrescriptionDate(), after.getPrescriptionDate())) {
            changed.add("date");
        }
        if (!Objects.equals(before.getDiagnosis(), after.getDiagnosis())) {
            changed.add("diagnosis");
        }
        if (!Objects.equals(before.getNotes(), after.getNotes())) {
            changed.add("notes");
        }
        if (itemsDiffer(before.getItems(), after.getItems())) {
            changed.add("medicine items (" + before.getItems().size() + " -> " + after.getItems().size() + ")");
        }
        if (before.getStatus() != after.getStatus()) {
            changed.add("status " + before.getStatus().getLabel() + " -> " + after.getStatus().getLabel());
        }
        return changed.isEmpty() ? "no changes" : "changed " + String.join(", ", changed);
    }

    private static boolean itemsDiffer(List<PrescriptionItem> a, List<PrescriptionItem> b) {
        if (a.size() != b.size()) {
            return true;
        }
        for (int i = 0; i < a.size(); i++) {
            PrescriptionItem x = a.get(i);
            PrescriptionItem y = b.get(i);
            if (!Objects.equals(x.getMedicineId(), y.getMedicineId()) || !Objects.equals(x.getDosage(), y.getDosage())
                    || !Objects.equals(x.getFrequency(), y.getFrequency()) || !Objects.equals(x.getDuration(), y.getDuration())
                    || !Objects.equals(x.getInstructions(), y.getInstructions()) || !Objects.equals(x.getQuantity(), y.getQuantity())) {
                return true;
            }
        }
        return false;
    }

    /** Confirms a draft so that it becomes available for dispensing. */
    public void issue(User actor, long id) {
        require(actor, Permission.PRESCRIPTION_MANAGE);
        tx.inTransaction(con -> {
            PrescriptionStatus current = prescriptionDao.lockAndGetStatus(con, id)
                    .orElseThrow(() -> new NotFoundException("Prescription"));
            if (current != PrescriptionStatus.DRAFT) {
                throw new BusinessRuleException("Only a draft prescription can be issued.");
            }
            Prescription p = prescriptionDao.findById(con, id).orElseThrow(() -> new NotFoundException("Prescription"));
            validate(con, p, true);
            prescriptionDao.updateStatus(con, id, PrescriptionStatus.ISSUED);
            audit.log(con, actor, "PRESCRIPTION_ISSUED", p, "Issued prescription " + p.getPrescriptionNo());
            return null;
        });
    }

    /** Cancels a draft or issued prescription. Once medicines have been dispensed it cannot be cancelled. */
    public void cancel(User actor, long id, String reason) {
        require(actor, Permission.PRESCRIPTION_MANAGE);
        String why = clean(reason);
        new Validator().text("reason", why, 200, "Cancellation reason").throwIfInvalid();
        tx.inTransaction(con -> {
            PrescriptionStatus current = prescriptionDao.lockAndGetStatus(con, id)
                    .orElseThrow(() -> new NotFoundException("Prescription"));
            if (!current.isCancellable() || prescriptionDao.hasDispensings(con, id)) {
                throw new BusinessRuleException("A " + current.getLabel().toLowerCase()
                        + " prescription cannot be cancelled.");
            }
            Prescription p = prescriptionDao.findHeaderById(con, id).orElseThrow(() -> new NotFoundException("Prescription"));
            prescriptionDao.updateStatus(con, id, PrescriptionStatus.CANCELLED);
            audit.log(con, actor, "PRESCRIPTION_CANCELLED", p, "Cancelled prescription " + p.getPrescriptionNo() + ". Reason: " + why);
            return null;
        });
    }
}
