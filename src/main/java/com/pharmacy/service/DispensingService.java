package com.pharmacy.service;

import com.pharmacy.dao.AbstractDao;
import com.pharmacy.dao.BatchDao;
import com.pharmacy.dao.DispensingDao;
import com.pharmacy.dao.PrescriptionDao;
import com.pharmacy.dao.SequenceDao;
import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.exception.DataAccessException;
import com.pharmacy.exception.DuplicateOperationException;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.model.Dispensing;
import com.pharmacy.model.DispensingItem;
import com.pharmacy.model.MedicineBatch;
import com.pharmacy.model.Permission;
import com.pharmacy.model.PrescriptionItem;
import com.pharmacy.model.PrescriptionStatus;
import com.pharmacy.model.StockTransactionType;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hands out medicines against an issued prescription.
 *
 * <p>Everything in {@link #dispense} happens in one database transaction:
 * the prescription row is locked, the candidate batches are locked, stock is
 * deducted, the ledger and dispensing rows are written and the prescription
 * status is updated. If any step fails nothing is saved.</p>
 */
public class DispensingService extends BaseService {
    private final PrescriptionDao prescriptionDao;
    private final DispensingDao dispensingDao;
    private final BatchDao batchDao;
    private final SequenceDao sequenceDao;
    private final InventoryService inventory;
    private final StockAllocationStrategy allocationStrategy;

    public DispensingService(TransactionManager tx, AuditService audit, PrescriptionDao prescriptionDao,
                             DispensingDao dispensingDao, BatchDao batchDao, SequenceDao sequenceDao,
                             InventoryService inventory, StockAllocationStrategy allocationStrategy) {
        super(tx, audit);
        this.prescriptionDao = prescriptionDao;
        this.dispensingDao = dispensingDao;
        this.batchDao = batchDao;
        this.sequenceDao = sequenceDao;
        this.inventory = inventory;
        this.allocationStrategy = allocationStrategy;
    }

    public Page<Dispensing> search(User actor, PageRequest request, String billing, LocalDate from, LocalDate to) {
        require(actor, Permission.DISPENSE);
        return tx.read(con -> dispensingDao.search(con, request, billing, from, to));
    }

    public Dispensing get(User actor, long id) {
        require(actor, Permission.DISPENSE);
        return tx.read(con -> dispensingDao.findById(con, id)).orElseThrow(() -> new NotFoundException("Dispensing record"));
    }

    public List<Dispensing> findByPrescription(User actor, long prescriptionId) {
        require(actor, Permission.PRESCRIPTION_VIEW);
        return tx.read(con -> dispensingDao.findByPrescription(con, prescriptionId));
    }

    /**
     * @param quantities   prescription item id -> units to hand out now (zero / missing = skip the item)
     * @param requestToken unique token rendered into the form; makes a repeated submission harmless
     * @return the saved dispensing
     */
    public Dispensing dispense(User actor, long prescriptionId, Map<Long, Integer> quantities, String requestToken, String notes) {
        require(actor, Permission.DISPENSE);
        String token = clean(requestToken);
        String note = clean(notes);
        new Validator().check(token != null && token.length() >= 16 && token.length() <= 64, "requestToken",
                        "The form has expired. Please reload the page and try again.")
                .maxLength("notes", note, 300, "Notes").throwIfInvalid();
        try {
            return tx.inTransaction(con -> {
                // 1. Serialize all work on this prescription.
                PrescriptionStatus status = prescriptionDao.lockAndGetStatus(con, prescriptionId)
                        .orElseThrow(() -> new NotFoundException("Prescription"));

                // 2. Was this exact form already processed? (double click, browser retry, back + resubmit)
                Optional<Long> already = dispensingDao.findIdByToken(con, token);
                if (already.isPresent()) {
                    throw new DuplicateOperationException("This dispensing was already recorded; stock was not deducted again.",
                            already.get());
                }
                if (!status.isDispensable()) {
                    throw new BusinessRuleException("Only issued prescriptions can be dispensed. This prescription is "
                            + status.getLabel().toLowerCase() + ".");
                }

                LocalDate today = LocalDate.now();
                List<PrescriptionItem> items = prescriptionDao.findItems(con, prescriptionId, today);
                String prescriptionNo = prescriptionDao.findHeaderById(con, prescriptionId)
                        .orElseThrow(() -> new NotFoundException("Prescription")).getPrescriptionNo();

                // 3. Validate requested quantities against what is still owed.
                Validator v = new Validator();
                int totalRequested = 0;
                for (Map.Entry<Long, Integer> e : quantities.entrySet()) {
                    boolean belongs = items.stream().anyMatch(i -> i.getId().equals(e.getKey()));
                    v.check(belongs, "items", "The form contains a medicine that is not part of this prescription.");
                }
                for (PrescriptionItem item : items) {
                    Integer qty = quantities.get(item.getId());
                    if (qty == null || qty == 0) {
                        continue;
                    }
                    v.check(qty > 0, "qty_" + item.getId(), item.getMedicineName() + ": quantity cannot be negative.");
                    v.check(qty <= item.getRemainingQuantity(), "qty_" + item.getId(), item.getMedicineName()
                            + ": only " + item.getRemainingQuantity() + " unit(s) remain to be dispensed.");
                    totalRequested += Math.max(qty, 0);
                }
                v.check(v.hasErrors() || totalRequested > 0, "items", "Enter a quantity for at least one medicine.");
                v.throwIfInvalid();

                // 4. Create the dispensing header.
                Dispensing dispensing = new Dispensing();
                dispensing.setDispensingNo(sequenceDao.nextCode(con, "DISPENSING", "DSP"));
                dispensing.setPrescriptionId(prescriptionId);
                dispensing.setPrescriptionNo(prescriptionNo);
                dispensing.setDispensedBy(actor.getId());
                dispensing.setDispensedAt(AbstractDao.now());
                dispensing.setRequestToken(token);
                dispensing.setNotes(note);
                dispensing.setId(dispensingDao.insert(con, dispensing));

                // 5. Allocate each item from locked, non-expired batches (FEFO) and deduct stock.
                int units = 0;
                for (PrescriptionItem item : items) {
                    Integer qty = quantities.get(item.getId());
                    if (qty == null || qty == 0) {
                        continue;
                    }
                    List<MedicineBatch> batches = batchDao.lockEligibleBatches(con, item.getMedicineId(), today);
                    for (StockAllocationStrategy.Allocation a : allocationStrategy.allocate(batches, qty, item.getMedicineName(), today)) {
                        DispensingItem line = new DispensingItem();
                        line.setPrescriptionItemId(item.getId());
                        line.setMedicineId(item.getMedicineId());
                        line.setBatchId(a.batch().getId());
                        line.setQuantity(a.quantity());
                        line.setUnitPrice(a.batch().getSellingPrice());
                        dispensingDao.insertItem(con, dispensing.getId(), line);
                        inventory.recordMovement(con, actor, a.batch(), StockTransactionType.DISPENSE, a.quantity(),
                                "Dispensed against " + prescriptionNo, "DISPENSING", dispensing.getId());
                    }
                    item.setDispensedQuantity(item.getDispensedQuantity() + qty);
                    units += qty;
                }

                // 6. The prescription is complete only when every item has been handed out in full.
                boolean complete = items.stream().allMatch(PrescriptionItem::isFullyDispensed);
                prescriptionDao.updateStatus(con, prescriptionId,
                        complete ? PrescriptionStatus.COMPLETED : PrescriptionStatus.PARTIALLY_DISPENSED);

                audit.log(con, actor, "DISPENSED", dispensing, "Dispensed " + units + " unit(s) against " + prescriptionNo
                        + " (" + dispensing.getDispensingNo() + (complete ? ", prescription completed)" : ", partial)"));
                return dispensing;
            });
        } catch (DataAccessException e) {
            // Two identical requests raced: the UNIQUE key on request_token rejected the second one.
            if (e.isDuplicateKey()) {
                Optional<Long> existing = tx.read(con -> dispensingDao.findIdByToken(con, token));
                if (existing.isPresent()) {
                    throw new DuplicateOperationException("This dispensing was already recorded; stock was not deducted again.",
                            existing.get());
                }
            }
            throw e;
        }
    }
}
