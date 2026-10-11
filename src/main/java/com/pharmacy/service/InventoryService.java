package com.pharmacy.service;

import com.pharmacy.dao.BatchDao;
import com.pharmacy.dao.MedicineDao;
import com.pharmacy.dao.StockTransactionDao;
import com.pharmacy.dao.SupplierDao;
import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.exception.ExpiredBatchException;
import com.pharmacy.exception.InsufficientStockException;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.exception.ValidationException;
import com.pharmacy.model.Medicine;
import com.pharmacy.model.MedicineBatch;
import com.pharmacy.model.Permission;
import com.pharmacy.model.StockTransaction;
import com.pharmacy.model.StockTransactionType;
import com.pharmacy.model.Supplier;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Every change to stock goes through this class (or the dispensing service,
 * which reuses {@link #recordMovement}). A change always consists of two
 * writes in ONE transaction: the batch quantity and a ledger row.
 */
public class InventoryService extends BaseService {
    private final MedicineDao medicineDao;
    private final BatchDao batchDao;
    private final StockTransactionDao stockTransactionDao;
    private final SupplierDao supplierDao;
    private final SettingsService settings;

    public InventoryService(TransactionManager tx, AuditService audit, MedicineDao medicineDao, BatchDao batchDao,
                            StockTransactionDao stockTransactionDao, SupplierDao supplierDao, SettingsService settings) {
        super(tx, audit);
        this.medicineDao = medicineDao;
        this.batchDao = batchDao;
        this.stockTransactionDao = stockTransactionDao;
        this.supplierDao = supplierDao;
        this.settings = settings;
    }

    // ------------------------------------------------------------------ reads

    public Page<MedicineBatch> searchBatches(User actor, PageRequest request, String status, Long supplierId) {
        require(actor, Permission.MEDICINE_VIEW);
        return tx.read(con -> batchDao.search(con, request, status, LocalDate.now(), settings.getNearExpiryDays(), supplierId));
    }

    public List<MedicineBatch> getBatches(User actor, long medicineId) {
        require(actor, Permission.MEDICINE_VIEW);
        return tx.read(con -> batchDao.findByMedicine(con, medicineId));
    }

    public MedicineBatch getBatch(User actor, long batchId) {
        require(actor, Permission.MEDICINE_VIEW);
        return tx.read(con -> batchDao.findById(con, batchId)).orElseThrow(() -> new NotFoundException("Batch"));
    }

    public Page<StockTransaction> searchTransactions(User actor, PageRequest request, StockTransactionType type,
                                                     LocalDate from, LocalDate to, Long medicineId) {
        require(actor, Permission.MEDICINE_VIEW);
        return tx.read(con -> stockTransactionDao.search(con, request, type, from, to, medicineId));
    }

    public List<StockTransaction> getRecentTransactions(User actor, long medicineId, int limit) {
        require(actor, Permission.MEDICINE_VIEW);
        return tx.read(con -> stockTransactionDao.findRecentForMedicine(con, medicineId, limit));
    }

    // --------------------------------------------------------------- stock in

    /**
     * Receives stock. A new batch number creates a batch; an existing batch
     * number of the same medicine is topped up (its expiry date must match).
     *
     * @param quantity passed separately so missing / invalid input becomes a field error
     */
    public MedicineBatch stockIn(User actor, MedicineBatch input, Integer quantity) {
        require(actor, Permission.STOCK_IN);
        input.setBatchNumber(clean(input.getBatchNumber()));
        LocalDate today = LocalDate.now();
        Validator v = new Validator()
                .required("medicineId", input.getMedicineId(), "Medicine")
                .text("batchNumber", input.getBatchNumber(), 50, "Batch number")
                .required("expiryDate", input.getExpiryDate(), "Expiry date")
                .check(input.getExpiryDate() == null || !input.getExpiryDate().isBefore(today), "expiryDate",
                        "Expired stock cannot be received.")
                .positive("quantity", quantity, "Quantity")
                .nonNegative("purchasePrice", input.getPurchasePrice(), "Purchase price")
                .nonNegative("sellingPrice", input.getSellingPrice(), "Selling price")
                .required("receivedDate", input.getReceivedDate(), "Received date")
                .notFuture("receivedDate", input.getReceivedDate(), "Received date");
        v.throwIfInvalid();

        return tx.inTransaction(con -> {
            Medicine medicine = medicineDao.findById(con, input.getMedicineId())
                    .orElseThrow(() -> new ValidationException("medicineId", "Select a valid medicine."));
            if (!medicine.isActive()) {
                throw new ValidationException("medicineId", "This medicine is deactivated; reactivate it before receiving stock.");
            }
            String supplierName = "";
            if (input.getSupplierId() != null) {
                Supplier supplier = supplierDao.findById(con, input.getSupplierId())
                        .orElseThrow(() -> new ValidationException("supplierId", "Select a valid supplier."));
                if (!supplier.isActive()) {
                    throw new ValidationException("supplierId", "This supplier is deactivated.");
                }
                supplierName = " from " + supplier.getSupplierCode();
            }

            Optional<MedicineBatch> existing = batchDao.findByNumberForUpdate(con, medicine.getId(), input.getBatchNumber());
            MedicineBatch batch;
            if (existing.isPresent()) {
                batch = existing.get();
                if (!batch.getExpiryDate().equals(input.getExpiryDate())) {
                    throw new ValidationException("expiryDate", "Batch " + batch.getBatchNumber()
                            + " already exists with expiry date " + batch.getExpiryDate() + ".");
                }
                batchDao.increaseQuantity(con, batch.getId(), quantity);
                batch.setQuantity(batch.getQuantity() + quantity);
            } else {
                batch = input;
                batch.setQuantity(quantity);
                batch.setId(batchDao.insert(con, batch));
            }
            writeLedger(con, actor, batch, StockTransactionType.STOCK_IN, quantity, batch.getQuantity(),
                    "Stock received" + supplierName, "BATCH", batch.getId());
            audit.log(con, actor.getId(), "STOCK_IN", "BATCH", batch.getId(), "Received " + quantity + " x "
                    + medicine.getMedicineCode() + " batch " + batch.getBatchNumber() + supplierName);
            return batch;
        });
    }

    // ------------------------------------------------- stock out / adjustment

    /** Removes stock for a non-sale reason (damage, disposal of expired stock, return to supplier...). */
    public void stockOut(User actor, long batchId, Integer quantity, String reason) {
        require(actor, Permission.STOCK_OUT);
        manualMovement(actor, batchId, StockTransactionType.STOCK_OUT, quantity, reason);
    }

    /** Corrects a counting difference. Restricted to administrators and always needs a reason. */
    public void adjust(User actor, long batchId, boolean increase, Integer quantity, String reason) {
        require(actor, Permission.STOCK_ADJUST);
        manualMovement(actor, batchId, increase ? StockTransactionType.ADJUSTMENT_IN : StockTransactionType.ADJUSTMENT_OUT,
                quantity, reason);
    }

    private void manualMovement(User actor, long batchId, StockTransactionType type, Integer quantity, String reason) {
        String why = clean(reason);
        new Validator().positive("quantity", quantity, "Quantity").text("reason", why, 300, "Reason").throwIfInvalid();
        tx.inTransaction(con -> {
            MedicineBatch batch = batchDao.findByIdForUpdate(con, batchId).orElseThrow(() -> new NotFoundException("Batch"));
            recordMovement(con, actor, batch, type, quantity, why, "MANUAL", null);
            audit.log(con, actor.getId(), type.name(), "BATCH", batch.getId(), type.getLabel() + " of " + quantity
                    + " on batch " + batch.getBatchNumber() + " (" + batch.getMedicineCode() + ")");
            return null;
        });
    }

    /**
     * Applies one movement to a batch that the caller has already locked
     * (SELECT ... FOR UPDATE) and appends the matching ledger row.
     * Stock can never go below zero: the UPDATE itself is guarded and the
     * whole transaction is rolled back by the exception.
     */
    void recordMovement(Connection con, User actor, MedicineBatch batch, StockTransactionType type, int quantity,
                        String reason, String referenceType, Long referenceId) throws SQLException {
        if (quantity <= 0) {
            throw new BusinessRuleException("Quantity must be greater than zero.");
        }
        // Defence in depth: whatever the allocation strategy chose, an expired batch is never dispensed.
        if (type == StockTransactionType.DISPENSE && batch.isExpiredOn(LocalDate.now())) {
            throw new ExpiredBatchException(batch.getBatchNumber());
        }
        if (type.isInbound()) {
            batchDao.increaseQuantity(con, batch.getId(), quantity);
        } else if (!batchDao.decreaseQuantity(con, batch.getId(), quantity)) {
            throw new InsufficientStockException(batch.getMedicineName() + " batch " + batch.getBatchNumber(),
                    quantity, batch.getQuantity());
        }
        int balance = type.apply(batch.getQuantity(), quantity);
        batch.setQuantity(balance);
        writeLedger(con, actor, batch, type, quantity, balance, reason, referenceType, referenceId);
    }

    private void writeLedger(Connection con, User actor, MedicineBatch batch, StockTransactionType type, int quantity,
                             int balanceAfter, String reason, String referenceType, Long referenceId) throws SQLException {
        StockTransaction t = new StockTransaction();
        t.setBatchId(batch.getId());
        t.setMedicineId(batch.getMedicineId());
        t.setType(type);
        t.setQuantity(quantity);
        t.setBalanceAfter(balanceAfter);
        t.setReason(reason);
        t.setReferenceType(referenceType);
        t.setReferenceId(referenceId);
        t.setUserId(actor.getId());
        stockTransactionDao.insert(con, t);
    }
}
