package com.pharmacy.dao;

import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class of all DAOs. It owns the repetitive JDBC plumbing
 * (PreparedStatement creation, parameter binding, try-with-resources,
 * paging) so that concrete DAOs only contain SQL and row mapping.
 *
 * @param <T> the entity type the concrete DAO mainly works with
 */
public abstract class AbstractDao<T> {

    /** Maps one row of this DAO's standard SELECT to an entity. */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    // ---------------------------------------------------------------- queries

    protected List<T> query(Connection con, String sql, Object... params) throws SQLException {
        return query(con, sql, this::mapRow, params);
    }

    protected <R> List<R> query(Connection con, String sql, RowMapper<R> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<R> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        }
    }

    protected Optional<T> queryOne(Connection con, String sql, Object... params) throws SQLException {
        List<T> rows = query(con, sql, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    protected long queryLong(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    /** Runs a paged query: one COUNT(*) and one LIMIT/OFFSET select sharing the same FROM + WHERE. */
    protected <R> Page<R> page(Connection con, String select, String from, Where where, String orderBy,
                               PageRequest request, RowMapper<R> mapper) throws SQLException {
        Object[] filterParams = where.params().toArray();
        long total = queryLong(con, "SELECT COUNT(*) " + from + where.sql(), filterParams);
        List<Object> params = new ArrayList<>(where.params());
        params.add(request.getSize());
        params.add(request.getOffset());
        List<R> items = query(con, select + " " + from + where.sql() + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?",
                mapper, params.toArray());
        return new Page<>(items, request.getPage(), request.getSize(), total);
    }

    protected Page<T> page(Connection con, String select, String from, Where where, String orderBy,
                           PageRequest request) throws SQLException {
        return page(con, select, from, where, orderBy, request, this::mapRow);
    }

    // ---------------------------------------------------------------- updates

    protected int update(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Executes an INSERT and returns the AUTO_INCREMENT key. */
    protected long insertAndReturnKey(Connection con, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Insert did not return a generated key.");
                }
                return keys.getLong(1);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    protected static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int index = i + 1;
            if (p instanceof LocalDate d) {
                ps.setDate(index, Date.valueOf(d));
            } else if (p instanceof LocalDateTime dt) {
                ps.setTimestamp(index, Timestamp.valueOf(dt));
            } else if (p instanceof Enum<?> e) {
                ps.setString(index, e.name());
            } else if (p instanceof Boolean b) {
                ps.setBoolean(index, b);
            } else {
                ps.setObject(index, p);
            }
        }
    }

    protected static LocalDate date(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d == null ? null : d.toLocalDate();
    }

    protected static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t == null ? null : t.toLocalDateTime();
    }

    protected static Long longOrNull(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    /** Current time without fractional seconds, matching MySQL DATETIME precision. */
    public static LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }
}
