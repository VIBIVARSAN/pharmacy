package com.pharmacy.service;

import com.pharmacy.dao.AbstractDao;
import com.pharmacy.dao.DispensingDao;
import com.pharmacy.dao.InvoiceDao;
import com.pharmacy.dao.SequenceDao;
import com.pharmacy.exception.BusinessRuleException;
import com.pharmacy.exception.DataAccessException;
import com.pharmacy.exception.DuplicateOperationException;
import com.pharmacy.exception.NotFoundException;
import com.pharmacy.model.Dispensing;
import com.pharmacy.model.DispensingItem;
import com.pharmacy.model.Invoice;
import com.pharmacy.model.InvoiceItem;
import com.pharmacy.model.InvoiceStatus;
import com.pharmacy.model.Payment;
import com.pharmacy.model.PaymentStatus;
import com.pharmacy.model.Permission;
import com.pharmacy.model.User;
import com.pharmacy.util.Money;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.TransactionManager;
import com.pharmacy.util.Validator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Invoices and payments. Billing never touches stock: quantities were already
 * deducted when the medicines were dispensed, so an invoice only records money.
 *
 * <p>Cancellation rules: only an administrator can cancel, a reason is
 * mandatory, and an invoice with recorded payments cannot be cancelled. A
 * cancelled invoice is kept (status CANCELLED) and its dispensing becomes
 * billable again.</p>
 */
public class BillingService extends BaseService {
    private final InvoiceDao invoiceDao;
    private final DispensingDao dispensingDao;
    private final SequenceDao sequenceDao;
    private final SettingsService settings;
    private final InvoiceCalculator calculator = new InvoiceCalculator();

    public BillingService(TransactionManager tx, AuditService audit, InvoiceDao invoiceDao, DispensingDao dispensingDao,
                          SequenceDao sequenceDao, SettingsService settings) {
        super(tx, audit);
        this.invoiceDao = invoiceDao;
        this.dispensingDao = dispensingDao;
        this.sequenceDao = sequenceDao;
        this.settings = settings;
    }

    public Page<Invoice> search(User actor, PageRequest request, PaymentStatus paymentStatus, InvoiceStatus status,
                                LocalDate from, LocalDate to, Long patientId) {
        require(actor, Permission.BILLING);
        return tx.read(con -> invoiceDao.search(con, request, paymentStatus, status, from, to, patientId));
    }

    public Invoice get(User actor, long id) {
        require(actor, Permission.BILLING);
        return tx.read(con -> invoiceDao.findById(con, id)).orElseThrow(() -> new NotFoundException("Invoice"));
    }

    private BigDecimal requireTaxRate() {
        return settings.getTaxRate().orElseThrow(() -> new BusinessRuleException(
                "No tax rate has been configured. An administrator must set it in Settings (enter 0 if no tax applies) before invoices can be created."));
    }

    /** Builds the invoice lines for a dispensing using the given per-line discounts (may be empty). */
    private Invoice build(Dispensing dispensing, Map<Long, BigDecimal> discounts) {
        Invoice invoice = new Invoice();
        invoice.setDispensingId(dispensing.getId());
        invoice.setDispensingNo(dispensing.getDispensingNo());
        invoice.setPrescriptionId(dispensing.getPrescriptionId());
        invoice.setPrescriptionNo(dispensing.getPrescriptionNo());
        invoice.setPatientId(dispensing.getPatientId());
        invoice.setPatientName(dispensing.getPatientName());
        invoice.setPatientCode(dispensing.getPatientCode());
        boolean discountsEnabled = settings.isDiscountsEnabled();
        for (DispensingItem di : dispensing.getItems()) {
            InvoiceItem line = new InvoiceItem();
            line.setDispensingItemId(di.getId());
            line.setMedicineId(di.getMedicineId());
            line.setMedicineName(di.getMedicineName());
            line.setBatchId(di.getBatchId());
            line.setBatchNumber(di.getBatchNumber());
            line.setQuantity(di.getQuantity());
            line.setUnitPrice(di.getUnitPrice());
            BigDecimal discount = discountsEnabled ? discounts.get(di.getId()) : null;
            line.setDiscountAmount(discount == null ? Money.ZERO : discount);
            invoice.getItems().add(line);
        }
        calculator.calculate(invoice, requireTaxRate());
        return invoice;
    }

    /** What the invoice for a dispensing would look like (shown on the "create invoice" screen). Nothing is saved. */
    public Invoice preview(User actor, long dispensingId) {
        require(actor, Permission.BILLING);
        Dispensing dispensing = tx.read(con -> dispensingDao.findById(con, dispensingId))
                .orElseThrow(() -> new NotFoundException("Dispensing record"));
        if (dispensing.isBilled()) {
            throw new DuplicateOperationException("This dispensing has already been billed (" + dispensing.getInvoiceNo() + ").",
                    dispensing.getInvoiceId());
        }
        return build(dispensing, Map.of());
    }

    /**
     * Creates the invoice, its items and (optionally) a first payment in one transaction.
     * All amounts are recalculated here on the server; nothing sent by the browser is trusted for totals.
     */
    public Invoice createInvoice(User actor, long dispensingId, Map<Long, BigDecimal> discounts, String paymentMethod,
                                 BigDecimal paymentAmount, String paymentReference) {
        require(actor, Permission.BILLING);
        try {
            return tx.inTransaction(con -> {
                // Lock the dispensing so two invoices for it cannot be created at the same time.
                if (!dispensingDao.lockById(con, dispensingId)) {
                    throw new NotFoundException("Dispensing record");
                }
                Optional<Long> existing = invoiceDao.findActiveIdByDispensing(con, dispensingId);
                if (existing.isPresent()) {
                    throw new DuplicateOperationException("This dispensing has already been billed.", existing.get());
                }
                Dispensing dispensing = dispensingDao.findById(con, dispensingId)
                        .orElseThrow(() -> new NotFoundException("Dispensing record"));
                Invoice invoice = build(dispensing, discounts);
                invoice.setInvoiceNo(sequenceDao.nextCode(con, "INVOICE", "INV"));
                invoice.setCreatedBy(actor.getId());
                invoice.setCreatedAt(AbstractDao.now());
                invoice.setStatus(InvoiceStatus.ISSUED);
                invoice.setPaymentStatus(PaymentStatus.UNPAID);
                invoice.setId(invoiceDao.insert(con, invoice));
                for (InvoiceItem line : invoice.getItems()) {
                    line.setId(invoiceDao.insertItem(con, invoice.getId(), line));
                }
                audit.log(con, actor, "INVOICE_CREATED", invoice, "Created invoice " + invoice.getInvoiceNo()
                        + " for " + dispensing.getDispensingNo());
                if (paymentAmount != null && paymentAmount.signum() != 0) {
                    applyPayment(con, actor, invoice, paymentAmount, paymentMethod, paymentReference);
                }
                return invoice;
            });
        } catch (DataAccessException e) {
            if (e.isDuplicateKey()) {
                // The UNIQUE key on invoices.active_dispensing_id is the last line of defence.
                throw new DuplicateOperationException("This dispensing has already been billed.", null);
            }
            throw e;
        }
    }

    public void addPayment(User actor, long invoiceId, BigDecimal amount, String method, String reference) {
        require(actor, Permission.BILLING);
        tx.inTransaction(con -> {
            Invoice invoice = invoiceDao.findHeaderById(con, invoiceId, true).orElseThrow(() -> new NotFoundException("Invoice"));
            applyPayment(con, actor, invoice, amount, method, reference);
            return null;
        });
    }

    /** Validates and stores one payment and refreshes the invoice's payment status. Caller holds the invoice lock. */
    private void applyPayment(Connection con, User actor, Invoice invoice, BigDecimal amount, String method, String reference)
            throws SQLException {
        String ref = clean(reference);
        if (invoice.isCancelled()) {
            throw new BusinessRuleException("A cancelled invoice cannot receive payments.");
        }
        BigDecimal alreadyPaid = invoiceDao.sumPayments(con, invoice.getId());
        BigDecimal balance = invoice.getGrandTotal().subtract(alreadyPaid);
        Validator v = new Validator()
                .check(amount != null && amount.signum() > 0 && amount.scale() <= 2, "paymentAmount",
                        "Enter a payment amount greater than zero with at most 2 decimal places.")
                .check(method != null && settings.getPaymentMethods().contains(method), "paymentMethod",
                        "Select one of the configured payment methods.")
                .maxLength("paymentReference", ref, 100, "Payment reference");
        if (!v.hasErrors()) {
            v.check(balance.signum() > 0, "paymentAmount", "This invoice is already fully paid.");
            v.check(amount.compareTo(balance) <= 0, "paymentAmount",
                    "The payment is more than the balance due (" + balance.toPlainString() + ").");
        }
        v.throwIfInvalid();

        Payment payment = new Payment();
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(Money.scale(amount));
        payment.setMethod(method);
        payment.setReference(ref);
        payment.setReceivedBy(actor.getId());
        payment.setPaidAt(AbstractDao.now());
        invoiceDao.insertPayment(con, payment);

        PaymentStatus status = PaymentStatus.of(alreadyPaid.add(payment.getAmount()), invoice.getGrandTotal());
        invoiceDao.updatePaymentStatus(con, invoice.getId(), status);
        invoice.setPaymentStatus(status);
        audit.log(con, actor, "PAYMENT_RECORDED", invoice, "Recorded " + method + " payment on " + invoice.getInvoiceNo()
                + " (" + status.getLabel() + ")");
    }

    public void cancel(User actor, long invoiceId, String reason) {
        require(actor, Permission.INVOICE_CANCEL);
        String why = clean(reason);
        new Validator().text("reason", why, 300, "Cancellation reason").throwIfInvalid();
        tx.inTransaction(con -> {
            Invoice invoice = invoiceDao.findHeaderById(con, invoiceId, true).orElseThrow(() -> new NotFoundException("Invoice"));
            if (invoice.isCancelled()) {
                throw new BusinessRuleException("This invoice is already cancelled.");
            }
            if (invoiceDao.sumPayments(con, invoiceId).signum() > 0) {
                throw new BusinessRuleException("An invoice with recorded payments cannot be cancelled.");
            }
            invoiceDao.cancel(con, invoiceId, actor.getId(), AbstractDao.now(), why);
            audit.log(con, actor, "INVOICE_CANCELLED", invoice, "Cancelled invoice " + invoice.getInvoiceNo() + ". Reason: " + why);
            return null;
        });
    }
}
