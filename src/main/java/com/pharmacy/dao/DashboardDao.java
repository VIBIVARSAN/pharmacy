package com.pharmacy.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Aggregate queries behind the dashboard cards and charts. Every figure comes straight from MySQL. */
public class DashboardDao extends AbstractDao<BigDecimal> {

    @Override
    protected BigDecimal mapRow(ResultSet rs) throws SQLException {
        BigDecimal v = rs.getBigDecimal(1);
        return v == null ? BigDecimal.ZERO : v;
    }

    /** Total of non-cancelled invoices created in [from, toExclusive). */
    public BigDecimal salesBetween(Connection con, LocalDate from, LocalDate toExclusive) throws SQLException {
        return query(con, "SELECT COALESCE(SUM(grand_total), 0) FROM invoices WHERE status = 'ISSUED' "
                + "AND created_at >= ? AND created_at < ?", from.atStartOfDay(), toExclusive.atStartOfDay()).get(0);
    }

    /** Sales per calendar day; days without invoices are simply absent from the map. */
    public Map<LocalDate, BigDecimal> salesByDay(Connection con, LocalDate from, LocalDate toExclusive) throws SQLException {
        Map<LocalDate, BigDecimal> result = new LinkedHashMap<>();
        List<Object[]> rows = query(con, "SELECT DATE(created_at) AS day, SUM(grand_total) AS total FROM invoices "
                        + "WHERE status = 'ISSUED' AND created_at >= ? AND created_at < ? GROUP BY DATE(created_at) ORDER BY day",
                rs -> new Object[]{date(rs, "day"), rs.getBigDecimal("total")}, from.atStartOfDay(), toExclusive.atStartOfDay());
        for (Object[] row : rows) {
            result.put((LocalDate) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    /** Usable (non-expired) units on hand per medicine category. */
    public Map<String, Long> stockByCategory(Connection con, LocalDate today) throws SQLException {
        Map<String, Long> result = new LinkedHashMap<>();
        List<Object[]> rows = query(con, "SELECT m.category, SUM(b.quantity) AS units FROM medicine_batches b "
                        + "JOIN medicines m ON m.id = b.medicine_id WHERE m.active = 1 AND b.expiry_date >= ? "
                        + "GROUP BY m.category HAVING SUM(b.quantity) > 0 ORDER BY units DESC",
                rs -> new Object[]{rs.getString("category"), rs.getLong("units")}, today);
        for (Object[] row : rows) {
            result.put((String) row[0], (Long) row[1]);
        }
        return result;
    }
}
