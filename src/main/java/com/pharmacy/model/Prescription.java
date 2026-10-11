package com.pharmacy.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A prescription is composed of a patient, a doctor and a list of items
 * (composition: the items have no life outside their prescription).
 */
public class Prescription extends BaseEntity implements Auditable {
    private static final long serialVersionUID = 1L;

    private String prescriptionNo;
    private Patient patient;
    private Doctor doctor;
    private LocalDate prescriptionDate;
    private String diagnosis;
    private String notes;
    private PrescriptionStatus status = PrescriptionStatus.DRAFT;
    private Long createdBy;
    private String createdByName;
    private final List<PrescriptionItem> items = new ArrayList<>();

    @Override
    public String getAuditType() {
        return "PRESCRIPTION";
    }

    @Override
    public String getAuditLabel() {
        return prescriptionNo;
    }

    public void addItem(PrescriptionItem item) {
        items.add(item);
    }

    public List<PrescriptionItem> getItems() {
        return items;
    }

    public void setItems(List<PrescriptionItem> newItems) {
        items.clear();
        items.addAll(newItems);
    }

    /** True when every item has been handed out in full. */
    public boolean isFullyDispensed() {
        return !items.isEmpty() && items.stream().allMatch(PrescriptionItem::isFullyDispensed);
    }

    public Long getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public Long getDoctorId() {
        return doctor == null ? null : doctor.getId();
    }

    public String getPrescriptionNo() {
        return prescriptionNo;
    }

    public void setPrescriptionNo(String prescriptionNo) {
        this.prescriptionNo = prescriptionNo;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public LocalDate getPrescriptionDate() {
        return prescriptionDate;
    }

    public void setPrescriptionDate(LocalDate prescriptionDate) {
        this.prescriptionDate = prescriptionDate;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public void setStatus(PrescriptionStatus status) {
        this.status = status;
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
}
