package com.pharmacy.model;

public enum Role {
    ADMIN("Administrator"), PHARMACIST("Pharmacist");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
