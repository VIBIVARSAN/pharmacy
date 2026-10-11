package com.pharmacy.service;

import com.pharmacy.dao.MedicineDao;
import com.pharmacy.dao.SequenceDao;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.Permission;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** The medicine catalog. Quantities are handled by {@link InventoryService}. */
public class MedicineService extends BaseService {
    private final MedicineDao medicineDao;
    private final SequenceDao sequenceDao;

    public MedicineService(TransactionManager tx, AuditService audit, MedicineDao medicineDao, SequenceDao sequenceDao) {
        super(tx, audit);
        this.medicineDao = medicineDao;
        this.sequenceDao = sequenceDao;
    }

    public Page<Medicine> search(User actor, PageRequest request, String category, String stock, boolean includeInactive) {
        require(actor, Permission.MEDICINE_VIEW);
        return tx.read(con -> medicineDao.search(con, request, category, stock, includeInactive));
    }

    public List<Medicine> findAllActive(User actor) {
        require(actor, Permission.MEDICINE_VIEW);
        return tx.read(medicineDao::findAllActive);
    }

    public List<String> getCategories(User actor) {
        require(actor, Permission.MEDICINE_VIEW);
        return tx.read(medicineDao::findCategories);
    }

    public Medicine get(User actor, long id) {
        require(actor, Permission.MEDICINE_VIEW);
        return tx.read(con -> medicineDao.findById(con, id)).orElseThrow(() -> new NotFoundException("Medicine"));
    }

    private void validate(Connection con, Medicine m, Integer reorderLevel) throws SQLException {
        m.setName(clean(m.getName()));
        m.setGenericName(clean(m.getGenericName()));
        m.setCategory(clean(m.getCategory()));
        m.setManufacturer(clean(m.getManufacturer()));
        m.setDosageForm(clean(m.getDosageForm()));
        m.setStrength(clean(m.getStrength()));
        m.setUnit(clean(m.getUnit()));
        m.setDescription(clean(m.getDescription()));
        new Validator().text("name", m.getName(), 150, "Medicine name")
                .maxLength("genericName", m.getGenericName(), 150, "Generic name")
                .text("category", m.getCategory(), 80, "Category")
                .maxLength("manufacturer", m.getManufacturer(), 150, "Manufacturer")
                .text("dosageForm", m.getDosageForm(), 50, "Dosage form")
                .maxLength("strength", m.getStrength(), 50, "Strength")
                .text("unit", m.getUnit(), 30, "Unit of measurement")
                .nonNegative("reorderLevel", reorderLevel, "Reorder level")
                .maxLength("description", m.getDescription(), 500, "Description")
                .throwIfInvalid();
        m.setReorderLevel(reorderLevel);
        if (medicineDao.existsSame(con, m)) {
            throw new ValidationException("name", "A medicine with the same name, strength and dosage form already exists.");
        }
    }

    /** @param reorderLevel passed separately so a missing / non-numeric value can be reported as a field error */
    public Medicine create(User actor, Medicine m, Integer reorderLevel) {
        require(actor, Permission.MEDICINE_MANAGE);
        return tx.inTransaction(con -> {
            validate(con, m, reorderLevel);
            m.setMedicineCode(sequenceDao.nextCode(con, "MEDICINE", "MED"));
            m.setActive(true);
            m.setId(medicineDao.insert(con, m));
            audit.log(con, actor, "MEDICINE_CREATED", m, "Added medicine " + m.getMedicineCode() + " " + m.getLabel());
            return m;
        });
    }

    public void update(User actor, Medicine m, Integer reorderLevel) {
        require(actor, Permission.MEDICINE_MANAGE);
        tx.inTransaction(con -> {
            Medicine existing = medicineDao.findById(con, m.getId()).orElseThrow(() -> new NotFoundException("Medicine"));
            validate(con, m, reorderLevel);
            medicineDao.update(con, m);
            audit.log(con, actor, "MEDICINE_UPDATED", existing, "Updated medicine " + existing.getMedicineCode());
            return null;
        });
    }

    public void setActive(User actor, long id, boolean active) {
        require(actor, Permission.MEDICINE_MANAGE);
        tx.inTransaction(con -> {
            Medicine existing = medicineDao.findById(con, id).orElseThrow(() -> new NotFoundException("Medicine"));
            medicineDao.setActive(con, id, active);
            audit.log(con, actor, active ? "MEDICINE_REACTIVATED" : "MEDICINE_DEACTIVATED", existing,
                    (active ? "Reactivated" : "Deactivated") + " medicine " + existing.getMedicineCode());
            return null;
        });
    }
}
