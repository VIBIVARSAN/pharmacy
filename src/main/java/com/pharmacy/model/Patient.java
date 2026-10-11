package com.pharmacy.model;

import java.time.LocalDate;
import java.time.Period;

public class Patient extends Person {
    private static final long serialVersionUID = 1L;

    private String patientCode;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String address;

    public Patient() {
    }

    /** Lightweight reference used when another object (a prescription) only needs to name the patient. */
    public Patient(Long id, String patientCode, String fullName) {
        super(id, fullName);
        this.patientCode = patientCode;
    }

    @Override
    public String getCode() {
        return patientCode;
    }

    @Override
    public String getAuditType() {
        return "PATIENT";
    }

    /** Age in completed years, or null when the date of birth is unknown. */
    public Integer getAge() {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
