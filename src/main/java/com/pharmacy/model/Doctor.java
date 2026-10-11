package com.pharmacy.model;

public class Doctor extends Person {
    private static final long serialVersionUID = 1L;

    private String doctorCode;
    private String specialization;
    private String hospital;
    private String registrationNo;

    public Doctor() {
    }

    public Doctor(Long id, String doctorCode, String fullName) {
        super(id, fullName);
        this.doctorCode = doctorCode;
    }

    @Override
    public String getCode() {
        return doctorCode;
    }

    @Override
    public String getAuditType() {
        return "DOCTOR";
    }

    /** Doctors are shown with their title - an overridden behaviour of Person. */
    @Override
    public String getDisplayName() {
        String name = getFullName();
        if (name == null) {
            return null;
        }
        return name.regionMatches(true, 0, "Dr", 0, 2) ? name : "Dr. " + name;
    }

    public String getDoctorCode() {
        return doctorCode;
    }

    public void setDoctorCode(String doctorCode) {
        this.doctorCode = doctorCode;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public void setRegistrationNo(String registrationNo) {
        this.registrationNo = registrationNo;
    }
}
