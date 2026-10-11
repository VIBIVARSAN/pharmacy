package com.pharmacy.service;

import com.pharmacy.dao.AuditLogDao;
import com.pharmacy.model.AuditLog;
import com.pharmacy.model.Auditable;
import com.pharmacy.model.Permission;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;
import com.pharmacy.util.RequestContext;
import com.pharmacy.util.TransactionManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Writes and reads the audit trail. Entries are written on the caller's
 * connection so an audit record commits (or rolls back) together with the
 * change it describes. Descriptions are short and never contain passwords,
 * tokens or clinical details.
 */
public class AuditService {
    private final TransactionManager tx;
    private final AuditLogDao auditLogDao;

    public AuditService(TransactionManager tx, AuditLogDao auditLogDao) {
        this.tx = tx;
        this.auditLogDao = auditLogDao;
    }

    /** Polymorphic variant: any {@link Auditable} entity describes itself. */
    public void log(Connection con, User actor, String action, Auditable entity, String description) throws SQLException {
        log(con, actor == null ? null : actor.getId(), action, entity.getAuditType(), entity.getId(), description);
    }

    public void log(Connection con, Long userId, String action, String entityType, Long entityId, String description)
            throws SQLException {
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setDescription(description.length() > 300 ? description.substring(0, 300) : description);
        entry.setIpAddress(RequestContext.getIpAddress());
        auditLogDao.insert(con, entry);
    }

    /** For events that are not part of another transaction (e.g. a failed login). */
    public void logStandalone(Long userId, String action, String entityType, Long entityId, String description) {
        tx.inTransaction(con -> {
            log(con, userId, action, entityType, entityId, description);
            return null;
        });
    }

    public Page<AuditLog> search(User actor, PageRequest request, String action, String entityType, LocalDate from, LocalDate to) {
        AccessControl.require(actor, Permission.AUDIT_VIEW);
        return tx.read(con -> auditLogDao.search(con, request, action, entityType, from, to));
    }

    public List<String> getActions(User actor) {
        AccessControl.require(actor, Permission.AUDIT_VIEW);
        return tx.read(auditLogDao::findActions);
    }

    public List<String> getEntityTypes(User actor) {
        AccessControl.require(actor, Permission.AUDIT_VIEW);
        return tx.read(auditLogDao::findEntityTypes);
    }
}
