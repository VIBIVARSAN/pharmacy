package com.pharmacy.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Every protected operation in the system and the roles allowed to perform it.
 * This is the single source of truth for authorization: services check it
 * before doing work and JSP pages use it to decide which controls to show.
 */
public enum Permission {
    PATIENT_VIEW(Role.ADMIN, Role.PHARMACIST),
    PATIENT_EDIT(Role.ADMIN, Role.PHARMACIST),
    PATIENT_DEACTIVATE(Role.ADMIN),
    DOCTOR_VIEW(Role.ADMIN, Role.PHARMACIST),
    DOCTOR_MANAGE(Role.ADMIN),
    SUPPLIER_VIEW(Role.ADMIN, Role.PHARMACIST),
    SUPPLIER_MANAGE(Role.ADMIN),
    MEDICINE_VIEW(Role.ADMIN, Role.PHARMACIST),
    MEDICINE_MANAGE(Role.ADMIN),
    STOCK_IN(Role.ADMIN, Role.PHARMACIST),
    STOCK_OUT(Role.ADMIN, Role.PHARMACIST),
    STOCK_ADJUST(Role.ADMIN),
    PRESCRIPTION_VIEW(Role.ADMIN, Role.PHARMACIST),
    PRESCRIPTION_MANAGE(Role.ADMIN),
    DISPENSE(Role.ADMIN, Role.PHARMACIST),
    BILLING(Role.ADMIN, Role.PHARMACIST),
    INVOICE_CANCEL(Role.ADMIN),
    REPORT_VIEW(Role.ADMIN),
    USER_MANAGE(Role.ADMIN),
    AUDIT_VIEW(Role.ADMIN),
    SETTINGS_MANAGE(Role.ADMIN);

    private final Set<Role> roles;

    Permission(Role first, Role... rest) {
        this.roles = EnumSet.of(first, rest);
    }

    public boolean isGrantedTo(Role role) {
        return role != null && roles.contains(role);
    }
}
