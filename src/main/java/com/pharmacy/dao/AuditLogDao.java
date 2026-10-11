package com.pharmacy.dao;

import com.pharmacy.model.AuditLog;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Audit entries are only ever inserted and read. */
public class AuditLogDao extends AbstractDao<AuditLog> {
    private static final String SELECT = "SELECT a.id, a.user_id, a.action, a.entity_type, a.entity_id, a.description, "
            + "a.ip_address, a.created_at, u.username";
    private static final String FROM = "FROM audit_logs a LEFT JOIN users u ON u.id = a.user_id";

    @Override
    protected AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog a = new AuditLog();
        a.setId(rs.getLong("id"));
        a.setUserId(longOrNull(rs, "user_id"));
        a.setUsername(rs.getString("username"));
        a.setAction(rs.getString("action"));
        a.setEntityType(rs.getString("entity_type"));
        a.setEntityId(longOrNull(rs, "entity_id"));
        a.setDescription(rs.getString("description"));
        a.setIpAddress(rs.getString("ip_address"));
        a.setCreatedAt(dateTime(rs, "created_at"));
        return a;
    }

    public void insert(Connection con, AuditLog a) throws SQLException {
        update(con, "INSERT INTO audit_logs (user_id, action, entity_type, entity_id, description, ip_address, created_at) "
                        + "VALUES (?,?,?,?,?,?,?)",
                a.getUserId(), a.getAction(), a.getEntityType(), a.getEntityId(), a.getDescription(), a.getIpAddress(), now());
    }

    public Page<AuditLog> search(Connection con, PageRequest request, String action, String entityType,
                                 LocalDate from, LocalDate to) throws SQLException {
        Where where = new Where().search(request.getQuery(), "a.description", "u.username")
                .andIf(action != null && !action.isBlank(), "a.action = ?", action)
                .andIf(entityType != null && !entityType.isBlank(), "a.entity_type = ?", entityType)
                .andIf(from != null, "a.created_at >= ?", from == null ? null : from.atStartOfDay())
                .andIf(to != null, "a.created_at < ?", to == null ? null : to.plusDays(1).atStartOfDay());
        return page(con, SELECT, FROM, where, "a.created_at DESC, a.id DESC", request);
    }

    public List<String> findActions(Connection con) throws SQLException {
        return query(con, "SELECT DISTINCT action FROM audit_logs ORDER BY action", rs -> rs.getString(1));
    }

    public List<String> findEntityTypes(Connection con) throws SQLException {
        return query(con, "SELECT DISTINCT entity_type FROM audit_logs ORDER BY entity_type", rs -> rs.getString(1));
    }
}
