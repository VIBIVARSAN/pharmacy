package com.pharmacy.service;

import com.pharmacy.dao.DoctorDao;
import com.pharmacy.dao.SequenceDao;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Doctor;
import com.pharmacy.model.Permission;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class DoctorService extends BaseService {
    private final DoctorDao doctorDao;
    private final SequenceDao sequenceDao;

    public DoctorService(TransactionManager tx, AuditService audit, DoctorDao doctorDao, SequenceDao sequenceDao) {
        super(tx, audit);
        this.doctorDao = doctorDao;
        this.sequenceDao = sequenceDao;
    }

    public Page<Doctor> search(User actor, PageRequest request, boolean includeInactive) {
        require(actor, Permission.DOCTOR_VIEW);
        return tx.read(con -> doctorDao.search(con, request, includeInactive));
    }

    public List<Doctor> lookup(User actor, String term) {
        require(actor, Permission.DOCTOR_VIEW);
        return tx.read(con -> doctorDao.lookup(con, term, 10));
    }

    public Doctor get(User actor, long id) {
        require(actor, Permission.DOCTOR_VIEW);
        return tx.read(con -> doctorDao.findById(con, id)).orElseThrow(() -> new NotFoundException("Doctor"));
    }

    private void validate(Connection con, Doctor d) throws SQLException {
        d.setFullName(clean(d.getFullName()));
        d.setSpecialization(clean(d.getSpecialization()));
        d.setPhone(clean(d.getPhone()));
        d.setEmail(clean(d.getEmail()));
        d.setHospital(clean(d.getHospital()));
        d.setRegistrationNo(clean(d.getRegistrationNo()));
        Validator v = new Validator().text("fullName", d.getFullName(), 120, "Full name")
                .text("specialization", d.getSpecialization(), 100, "Specialization")
                .required("phone", d.getPhone(), "Phone").phone("phone", d.getPhone())
                .email("email", d.getEmail())
                .maxLength("hospital", d.getHospital(), 150, "Hospital / clinic")
                .maxLength("registrationNo", d.getRegistrationNo(), 50, "Registration number");
        v.throwIfInvalid();
        if (d.getRegistrationNo() != null && doctorDao.registrationNoExists(con, d.getRegistrationNo(), d.getId())) {
            throw new ValidationException("registrationNo", "Another doctor already uses this registration number.");
        }
    }

    public Doctor create(User actor, Doctor d) {
        require(actor, Permission.DOCTOR_MANAGE);
        return tx.inTransaction(con -> {
            validate(con, d);
            d.setDoctorCode(sequenceDao.nextCode(con, "DOCTOR", "DOC"));
            d.setActive(true);
            d.setId(doctorDao.insert(con, d));
            audit.log(con, actor, "DOCTOR_CREATED", d, "Added doctor " + d.getDoctorCode());
            return d;
        });
    }

    public void update(User actor, Doctor d) {
        require(actor, Permission.DOCTOR_MANAGE);
        tx.inTransaction(con -> {
            Doctor existing = doctorDao.findById(con, d.getId()).orElseThrow(() -> new NotFoundException("Doctor"));
            validate(con, d);
            doctorDao.update(con, d);
            audit.log(con, actor, "DOCTOR_UPDATED", existing, "Updated doctor " + existing.getDoctorCode());
            return null;
        });
    }

    /** Deactivation keeps the row, so existing prescriptions still show their doctor. */
    public void setActive(User actor, long id, boolean active) {
        require(actor, Permission.DOCTOR_MANAGE);
        tx.inTransaction(con -> {
            Doctor existing = doctorDao.findById(con, id).orElseThrow(() -> new NotFoundException("Doctor"));
            doctorDao.setActive(con, id, active);
            audit.log(con, actor, active ? "DOCTOR_REACTIVATED" : "DOCTOR_DEACTIVATED", existing,
                    (active ? "Reactivated" : "Deactivated") + " doctor " + existing.getDoctorCode());
            return null;
        });
    }
}
