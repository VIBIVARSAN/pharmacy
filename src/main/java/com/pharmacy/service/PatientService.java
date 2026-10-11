package com.pharmacy.service;

import com.pharmacy.dao.PatientDao;
import com.pharmacy.dao.SequenceDao;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Patient;
import com.pharmacy.model.Permission;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PatientService extends BaseService {
    private final PatientDao patientDao;
    private final SequenceDao sequenceDao;

    public PatientService(TransactionManager tx, AuditService audit, PatientDao patientDao, SequenceDao sequenceDao) {
        super(tx, audit);
        this.patientDao = patientDao;
        this.sequenceDao = sequenceDao;
    }

    public Page<Patient> search(User actor, PageRequest request, boolean includeInactive) {
        require(actor, Permission.PATIENT_VIEW);
        return tx.read(con -> patientDao.search(con, request, includeInactive));
    }

    public List<Patient> lookup(User actor, String term) {
        require(actor, Permission.PATIENT_VIEW);
        return tx.read(con -> patientDao.lookup(con, term, 10));
    }

    public Patient get(User actor, long id) {
        require(actor, Permission.PATIENT_VIEW);
        return tx.read(con -> patientDao.findById(con, id)).orElseThrow(() -> new NotFoundException("Patient"));
    }

    private void validate(Patient p) {
        p.setFullName(clean(p.getFullName()));
        p.setPhone(clean(p.getPhone()));
        p.setEmail(clean(p.getEmail()));
        p.setAddress(clean(p.getAddress()));
        new Validator().text("fullName", p.getFullName(), 120, "Full name")
                .required("gender", p.getGender(), "Gender")
                .required("phone", p.getPhone(), "Phone").phone("phone", p.getPhone())
                .email("email", p.getEmail())
                .maxLength("address", p.getAddress(), 300, "Address")
                .notFuture("dateOfBirth", p.getDateOfBirth(), "Date of birth")
                .check(p.getDateOfBirth() == null || p.getDateOfBirth().isAfter(LocalDate.now().minusYears(130)),
                        "dateOfBirth", "Date of birth is not realistic.")
                .throwIfInvalid();
    }

    /** Registers a patient and assigns the next patient ID (PAT-000001, ...). */
    public Patient create(User actor, Patient p) {
        require(actor, Permission.PATIENT_EDIT);
        validate(p);
        return tx.inTransaction(con -> {
            rejectDuplicate(patientDao.findDuplicate(con, p.getFullName(), p.getPhone(), null));
            p.setPatientCode(sequenceDao.nextCode(con, "PATIENT", "PAT"));
            p.setActive(true);
            p.setId(patientDao.insert(con, p));
            audit.log(con, actor, "PATIENT_CREATED", p, "Registered patient " + p.getPatientCode());
            return p;
        });
    }

    public void update(User actor, Patient p) {
        require(actor, Permission.PATIENT_EDIT);
        validate(p);
        tx.inTransaction(con -> {
            Patient existing = patientDao.findById(con, p.getId()).orElseThrow(() -> new NotFoundException("Patient"));
            rejectDuplicate(patientDao.findDuplicate(con, p.getFullName(), p.getPhone(), p.getId()));
            patientDao.update(con, p);
            audit.log(con, actor, "PATIENT_UPDATED", existing, "Updated patient " + existing.getPatientCode());
            return null;
        });
    }

    /** Patients are never deleted (their history must stay intact); they are deactivated instead. */
    public void setActive(User actor, long id, boolean active) {
        require(actor, Permission.PATIENT_DEACTIVATE);
        tx.inTransaction(con -> {
            Patient existing = patientDao.findById(con, id).orElseThrow(() -> new NotFoundException("Patient"));
            patientDao.setActive(con, id, active);
            audit.log(con, actor, active ? "PATIENT_REACTIVATED" : "PATIENT_DEACTIVATED", existing,
                    (active ? "Reactivated" : "Deactivated") + " patient " + existing.getPatientCode());
            return null;
        });
    }

    private static void rejectDuplicate(Optional<Patient> duplicate) {
        if (duplicate.isPresent()) {
            throw new ValidationException("fullName", "A patient with this name and phone number already exists ("
                    + duplicate.get().getPatientCode() + ").");
        }
    }
}
