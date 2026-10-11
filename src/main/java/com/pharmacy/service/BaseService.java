package com.pharmacy.service;

import com.pharmacy.model.Permission;
import com.pharmacy.model.User;
import com.pharmacy.util.TransactionManager;

/**
 * Common parent of the business services: every service needs transactions,
 * auditing and permission checks, so they are provided once here.
 */
public abstract class BaseService {
    protected final TransactionManager tx;
    protected final AuditService audit;

    protected BaseService(TransactionManager tx, AuditService audit) {
        this.tx = tx;
        this.audit = audit;
    }

    protected void require(User actor, Permission permission) {
        AccessControl.require(actor, permission);
    }

    /** Trims a text field and turns empty input into null so blanks are never stored. */
    protected static String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
