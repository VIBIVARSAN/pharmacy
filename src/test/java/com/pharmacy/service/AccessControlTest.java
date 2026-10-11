package com.pharmacy.service;

import com.pharmacy.exception.AuthorizationException;
import com.pharmacy.model.Permission;
import com.pharmacy.model.Role;
import com.pharmacy.model.User;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessControlTest {

    private static User user(Role role, boolean active) {
        User u = new User();
        u.setId(1L);
        u.setRole(role);
        u.setActive(active);
        return u;
    }

    @Test
    void administratorHasEveryPermission() {
        User admin = user(Role.ADMIN, true);
        for (Permission p : Permission.values()) {
            assertTrue(admin.can(p), p.name());
        }
    }

    @Test
    void pharmacistHasExactlyTheDocumentedPermissions() {
        User pharmacist = user(Role.PHARMACIST, true);
        Set<Permission> expected = EnumSet.of(Permission.PATIENT_VIEW, Permission.PATIENT_EDIT, Permission.DOCTOR_VIEW,
                Permission.SUPPLIER_VIEW, Permission.MEDICINE_VIEW, Permission.STOCK_IN, Permission.STOCK_OUT,
                Permission.PRESCRIPTION_VIEW, Permission.DISPENSE, Permission.BILLING);
        Set<Permission> actual = EnumSet.noneOf(Permission.class);
        for (Permission p : Permission.values()) {
            if (pharmacist.can(p)) {
                actual.add(p);
            }
        }
        assertEquals(expected, actual);
    }

    @Test
    void deactivatedUsersAndMissingUsersAreDenied() {
        assertFalse(user(Role.ADMIN, false).can(Permission.PATIENT_VIEW));
        assertThrows(AuthorizationException.class, () -> AccessControl.require(null, Permission.PATIENT_VIEW));
        assertThrows(AuthorizationException.class, () -> AccessControl.require(user(Role.PHARMACIST, true), Permission.USER_MANAGE));
        assertDoesNotThrow(() -> AccessControl.require(user(Role.PHARMACIST, true), Permission.DISPENSE));
    }
}
