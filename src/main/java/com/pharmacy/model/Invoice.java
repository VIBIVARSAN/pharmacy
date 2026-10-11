package com.pharmacy.model;

import com.pharmacy.util.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** An invoice is composed of line items and the payments received against it. */
public class Invoice extends BaseEntity implements Auditable {
    private static final long serialVersionUID = 1L;

    private String invoiceNo;
    private Long patientId;
    private String patientName;
    private String patientCode;
    private String patientPhone;
    private String patientAddress;
    private Long dispensingId;
    private String dispensingNo;
    private Long prescriptionId;
    private String prescriptionNo;
    private BigDecimal subtotal = Money.ZERO;
    private BigDecimal discountAmount = Money.ZERO;
    private BigDecimal taxRate = Money.ZERO;
    private BigDecimal taxAmount = Money.ZERO;
    private BigDecimal grandTotal = Money.ZERO;
    private InvoiceStatus status = InvoiceStatus.ISSUED;
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private BigDecimal amountPaid = Money.ZERO;
    private final List<InvoiceItem> items = new ArrayList<>();
    private final List<Payment> payments = new ArrayList<>();

    @Override
    public String getAuditType() {
        return "INVOICE";
    }

    @Override
    public String getAuditLabel() {
        return invoiceNo;
    }

    /** Sum of the payments recorded against this invoice. */
    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = Money.scale(amountPaid);
    }

    public BigDecimal getBalanceDue() {
        return grandTotal.subtract(getAmountPaid()).max(Money.ZERO);
    }

    public boolean isCancelled() {
        return status == InvoiceStatus.CANCELLED;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> newItems) {
        items.clear();
        items.addAll(newItems);
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> newPayments) {
        payments.clear();
        payments.addAll(newPayments);
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    public Long getDispensingId() {
        return dispensingId;
    }

    public void setDispensingId(Long dispensingId) {
        this.dispensingId = dispensingId;
    }

    public String getDispensingNo() {
        return dispensingNo;
    }

    public void setDispensingNo(String dispensingNo) {
        this.dispensingNo = dispensingNo;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getPrescriptionNo() {
        return prescriptionNo;
    }

    public void setPrescriptionNo(String prescriptionNo) {
        this.prescriptionNo = prescriptionNo;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
