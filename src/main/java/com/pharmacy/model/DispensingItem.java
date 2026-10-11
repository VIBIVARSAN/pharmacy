package com.pharmacy.model;

import com.pharmacy.util.Money;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Quantity of one medicine taken from one specific batch during a dispensing. */
public class DispensingItem extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long dispensingId;
    private Long prescriptionItemId;
    private Long medicineId;
    private String medicineName;
    private Long batchId;
    private String batchNumber;
    private LocalDate expiryDate;
    private int quantity;
    private BigDecimal unitPrice;

    public BigDecimal getLineTotal() {
        return unitPrice == null ? Money.ZERO : Money.multiply(unitPrice, quantity);
    }

    public Long getDispensingId() {
        return dispensingId;
    }

    public void setDispensingId(Long dispensingId) {
        this.dispensingId = dispensingId;
    }

    public Long getPrescriptionItemId() {
        return prescriptionItemId;
    }

    public void setPrescriptionItemId(Long prescriptionItemId) {
        this.prescriptionItemId = prescriptionItemId;
    }

    public Long getMedicineId() {
        return medicineId;
    }

    public void setMedicineId(Long medicineId) {
        this.medicineId = medicineId;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
