package com.pharmacy.service;

import com.pharmacy.dao.SequenceDao;
import com.pharmacy.dao.SupplierDao;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.model.Permission;
import com.pharmacy.model.Supplier;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.util.List;

public class SupplierService extends BaseService {
    private final SupplierDao supplierDao;
    private final SequenceDao sequenceDao;

    public SupplierService(TransactionManager tx, AuditService audit, SupplierDao supplierDao, SequenceDao sequenceDao) {
        super(tx, audit);
        this.supplierDao = supplierDao;
        this.sequenceDao = sequenceDao;
    }

    public Page<Supplier> search(User actor, PageRequest request, boolean includeInactive) {
        require(actor, Permission.SUPPLIER_VIEW);
        return tx.read(con -> supplierDao.search(con, request, includeInactive));
    }

    public List<Supplier> findAllActive(User actor) {
        require(actor, Permission.SUPPLIER_VIEW);
        return tx.read(supplierDao::findAllActive);
    }

    public Supplier get(User actor, long id) {
        require(actor, Permission.SUPPLIER_VIEW);
        return tx.read(con -> supplierDao.findById(con, id)).orElseThrow(() -> new NotFoundException("Supplier"));
    }

    private void validate(Supplier s) {
        s.setName(clean(s.getName()));
        s.setContactPerson(clean(s.getContactPerson()));
        s.setPhone(clean(s.getPhone()));
        s.setEmail(clean(s.getEmail()));
        s.setAddress(clean(s.getAddress()));
        new Validator().text("name", s.getName(), 150, "Supplier name")
                .maxLength("contactPerson", s.getContactPerson(), 120, "Contact person")
                .required("phone", s.getPhone(), "Phone").phone("phone", s.getPhone())
                .email("email", s.getEmail())
                .maxLength("address", s.getAddress(), 300, "Address")
                .throwIfInvalid();
    }

    public Supplier create(User actor, Supplier s) {
        require(actor, Permission.SUPPLIER_MANAGE);
        validate(s);
        return tx.inTransaction(con -> {
            s.setSupplierCode(sequenceDao.nextCode(con, "SUPPLIER", "SUP"));
            s.setActive(true);
            s.setId(supplierDao.insert(con, s));
            audit.log(con, actor, "SUPPLIER_CREATED", s, "Added supplier " + s.getSupplierCode());
            return s;
        });
    }

    public void update(User actor, Supplier s) {
        require(actor, Permission.SUPPLIER_MANAGE);
        validate(s);
        tx.inTransaction(con -> {
            Supplier existing = supplierDao.findById(con, s.getId()).orElseThrow(() -> new NotFoundException("Supplier"));
            supplierDao.update(con, s);
            audit.log(con, actor, "SUPPLIER_UPDATED", existing, "Updated supplier " + existing.getSupplierCode());
            return null;
        });
    }

    public void setActive(User actor, long id, boolean active) {
        require(actor, Permission.SUPPLIER_MANAGE);
        tx.inTransaction(con -> {
            Supplier existing = supplierDao.findById(con, id).orElseThrow(() -> new NotFoundException("Supplier"));
            supplierDao.setActive(con, id, active);
            audit.log(con, actor, active ? "SUPPLIER_REACTIVATED" : "SUPPLIER_DEACTIVATED", existing,
                    (active ? "Reactivated" : "Deactivated") + " supplier " + existing.getSupplierCode());
            return null;
        });
    }
}
