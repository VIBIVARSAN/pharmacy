package com.pharmacy.dao;

import com.pharmacy.model.Role;
import com.pharmacy.model.User;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

public class UserDao extends AbstractDao<User> implements CrudDao<User> {
    private static final String SELECT = "SELECT id, username, password_hash, full_name, email, phone, role, active, "
            + "last_login_at, created_at, updated_at";
    private static final Map<String, String> SORTS = Map.of("username", "username", "name", "full_name",
            "role", "role", "lastLogin", "last_login_at");

    @Override
    protected User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        u.setLastLoginAt(dateTime(rs, "last_login_at"));
        u.setCreatedAt(dateTime(rs, "created_at"));
        u.setUpdatedAt(dateTime(rs, "updated_at"));
        return u;
    }

    @Override
    public Optional<User> findById(Connection con, long id) throws SQLException {
        return queryOne(con, SELECT + " FROM users WHERE id = ?", id);
    }

    public Optional<User> findByUsername(Connection con, String username) throws SQLException {
        return queryOne(con, SELECT + " FROM users WHERE username = ?", username);
    }

    public long countAll(Connection con) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM users");
    }

    /** Locks the active administrators so "last admin" checks are safe under concurrency. */
    public long countActiveAdminsForUpdate(Connection con) throws SQLException {
        return query(con, "SELECT id FROM users WHERE role = 'ADMIN' AND active = 1 FOR UPDATE", rs -> rs.getLong(1)).size();
    }

    /**
     * Takes a row lock on a row that always exists, acting as a mutex for the
     * one-time setup so two simultaneous requests cannot both create a "first" admin.
     */
    public void acquireSetupLock(Connection con) throws SQLException {
        query(con, "SELECT name FROM sequences WHERE name = 'PATIENT' FOR UPDATE", rs -> rs.getString(1));
    }

    @Override
    public long insert(Connection con, User u) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO users (username, password_hash, full_name, email, phone, role, active) VALUES (?,?,?,?,?,?,?)",
                u.getUsername(), u.getPasswordHash(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(), u.isActive());
    }

    @Override
    public void update(Connection con, User u) throws SQLException {
        update(con, "UPDATE users SET full_name = ?, email = ?, phone = ?, role = ?, active = ? WHERE id = ?",
                u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(), u.isActive(), u.getId());
    }

    public void updatePassword(Connection con, long id, String passwordHash) throws SQLException {
        update(con, "UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, id);
    }

    public void updateLastLogin(Connection con, long id, LocalDateTime when) throws SQLException {
        update(con, "UPDATE users SET last_login_at = ? WHERE id = ?", when, id);
    }

    public Page<User> search(Connection con, PageRequest request) throws SQLException {
        Where where = new Where().search(request.getQuery(), "username", "full_name", "email");
        return page(con, SELECT, "FROM users", where, request.orderBy(SORTS, "username ASC"), request);
    }
}
