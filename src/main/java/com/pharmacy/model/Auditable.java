package com.pharmacy.model;

/**
 * Implemented by entities whose changes are written to the audit log.
 * The audit service only depends on this small contract, not on concrete classes.
 */
public interface Auditable {
    Long getId();

    /** Stable name stored in audit_logs.entity_type, e.g. "PATIENT". */
    String getAuditType();

    /** Short human readable reference (a code or number - never clinical details). */
    String getAuditLabel();
}
