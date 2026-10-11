package com.pharmacy.model;

import com.pharmacy.util.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One hand-over of medicines against a prescription (a prescription may have several). */
public class Dispensing extends BaseEntity implements Auditable {
    private static final long serialVersionUID = 1L;

    private String dispensingNo;
    private Long prescriptionId;
    private String prescriptionNo;
    private Long patientId;
    private String patientName;
    private String patientCode;
    private Long dispensedBy;
    private String dispensedByName;
    private LocalDateTime dispensedAt;
    private String requestToken;
    private String notes;
    private Long invoiceId;
    private String invoiceNo;
    private BigDecimal totalAmount = Money.ZERO;
    private final List<DispensingItem> items = new ArrayList<>();

    @Override
    public String getAuditType() {
        return "DISPENSING";
    }

    @Override
    public String getAuditLabel() {
        return dispensingNo;
    }

    /** Value of the dispensed items (quantity x batch selling price), calculated by the database. */
    public BigDecimal getTotal() {
        return totalAmount;
    }

    public void setTotal(BigDecimal totalAmount) {
        this.totalAmount = Money.scale(totalAmount);
    }

    public boolean isBilled() {
        return invoiceId != null;
    }

    public List<DispensingItem> getItems() {
        return items;
    }

    public void setItems(List<DispensingItem> newItems) {
        items.clear();
        items.addAll(newItems);
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

    public Long getDispensedBy() {
        return dispensedBy;
    }

    public void setDispensedBy(Long dispensedBy) {
        this.dispensedBy = dispensedBy;
    }

    public String getDispensedByName() {
        return dispensedByName;
    }

    public void setDispensedByName(String dispensedByName) {
        this.dispensedByName = dispensedByName;
    }

    public LocalDateTime getDispensedAt() {
        return dispensedAt;
    }

    public void setDispensedAt(LocalDateTime dispensedAt) {
        this.dispensedAt = dispensedAt;
    }

    public String getRequestToken() {
        return requestToken;
    }

    public void setRequestToken(String requestToken) {
        this.requestToken = requestToken;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }
}
