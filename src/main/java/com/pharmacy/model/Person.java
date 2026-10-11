package com.pharmacy.model;

/**
 * Shared abstraction for the people the system knows about: patients, doctors
 * and application users. Each subclass supplies its own code and display name.
 */
public abstract class Person extends BaseEntity implements Auditable {
    private static final long serialVersionUID = 1L;

    private String fullName;
    private String phone;
    private String email;
    private boolean active = true;

    protected Person() {
    }

    protected Person(Long id, String fullName) {
        super(id);
        this.fullName = fullName;
    }

    /** The unique business identifier of this person (PAT-000001, DOC-000001, a username...). */
    public abstract String getCode();

    /** Name as it should appear in lists and documents. Subclasses may decorate it. */
    public String getDisplayName() {
        return fullName;
    }

    @Override
    public String getAuditLabel() {
        return getCode();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
