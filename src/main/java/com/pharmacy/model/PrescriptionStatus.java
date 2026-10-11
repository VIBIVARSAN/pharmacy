package com.pharmacy.model;

/** Workflow state of a prescription. The enum itself knows which actions each state allows. */
public enum PrescriptionStatus {
    DRAFT("Draft"),
    ISSUED("Issued"),
    PARTIALLY_DISPENSED("Partially dispensed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    PrescriptionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Clinical details may only change before any medicine has been handed out. */
    public boolean isEditable() {
        return this == DRAFT || this == ISSUED;
    }

    public boolean isDispensable() {
        return this == ISSUED || this == PARTIALLY_DISPENSED;
    }

    public boolean isCancellable() {
        return this == DRAFT || this == ISSUED;
    }
}
