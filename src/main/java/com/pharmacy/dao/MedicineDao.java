package com.pharmacy.dao;

import com.pharmacy.model.Medicine;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MedicineDao extends AbstractDao<Medicine> implements CrudDao<Medicine> {
    private static final String SELECT = "SELECT m.id, m.medicine_code, m.name, m.generic_name, m.category, m.manufacturer, "
            + "m.dosage_form, m.strength, m.unit, m.reorder_level, m.description, m.active, m.created_at, m.updated_at, "
            + "COALESCE(s.available, 0) AS available_stock, COALESCE(s.expired, 0) AS expired_stock";
    /** Stock is always derived from the batches; the two ? are "today". */
    private static final String FROM = "FROM medicines m LEFT JOIN (SELECT medicine_id, "
            + "SUM(CASE WHEN expiry_date >= ? THEN quantity ELSE 0 END) AS available, "
            + "SUM(CASE WHEN expiry_date < ? THEN quantity ELSE 0 END) AS expired "
            + "FROM medicine_batches GROUP BY medicine_id) s ON s.medicine_id = m.id";
    private static final Map<String, String> SORTS = Map.of("code", "m.medicine_code", "name", "m.name",
            "category", "m.category", "stock", "available_stock", "reorder", "m.reorder_level");

    @Override
    protected Medicine mapRow(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setId(rs.getLong("id"));
        m.setMedicineCode(rs.getString("medicine_code"));
        m.setName(rs.getString("name"));
        m.setGenericName(rs.getString("generic_name"));
        m.setCategory(rs.getString("category"));
        m.setManufacturer(rs.getString("manufacturer"));
        m.setDosageForm(rs.getString("dosage_form"));
        m.setStrength(rs.getString("strength"));
        m.setUnit(rs.getString("unit"));
        m.setReorderLevel(rs.getInt("reorder_level"));
        m.setDescription(rs.getString("description"));
        m.setActive(rs.getBoolean("active"));
        m.setCreatedAt(dateTime(rs, "created_at"));
        m.setUpdatedAt(dateTime(rs, "updated_at"));
        m.setAvailableStock(rs.getInt("available_stock"));
        m.setExpiredStock(rs.getInt("expired_stock"));
        return m;
    }

    @Override
    public Optional<Medicine> findById(Connection con, long id) throws SQLException {
        LocalDate today = LocalDate.now();
        return queryOne(con, SELECT + " " + FROM + " WHERE m.id = ?", today, today, id);
    }

    @Override
    public long insert(Connection con, Medicine m) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO medicines (medicine_code, name, generic_name, category, manufacturer, dosage_form, strength, unit, "
                        + "reorder_level, description, active, created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                m.getMedicineCode(), m.getName(), m.getGenericName(), m.getCategory(), m.getManufacturer(), m.getDosageForm(),
                m.getStrength(), m.getUnit(), m.getReorderLevel(), m.getDescription(), m.isActive(), now());
    }

    @Override
    public void update(Connection con, Medicine m) throws SQLException {
        update(con, "UPDATE medicines SET name = ?, generic_name = ?, category = ?, manufacturer = ?, dosage_form = ?, "
                        + "strength = ?, unit = ?, reorder_level = ?, description = ? WHERE id = ?",
                m.getName(), m.getGenericName(), m.getCategory(), m.getManufacturer(), m.getDosageForm(), m.getStrength(),
                m.getUnit(), m.getReorderLevel(), m.getDescription(), m.getId());
    }

    public void setActive(Connection con, long id, boolean active) throws SQLException {
        update(con, "UPDATE medicines SET active = ? WHERE id = ?", active, id);
    }

    /**
     * @param category optional exact category
     * @param stock    optional filter: "low" (at or below reorder level) or "out" (no usable stock)
     */
    public Page<Medicine> search(Connection con, PageRequest request, String category, String stock,
                                 boolean includeInactive) throws SQLException {
        LocalDate today = LocalDate.now();
        Where where = new Where();
        // the two date parameters of the stock sub-query come first
        where.params().add(today);
        where.params().add(today);
        where.search(request.getQuery(), "m.medicine_code", "m.name", "m.generic_name", "m.manufacturer")
                .andIf(category != null && !category.isBlank(), "m.category = ?", category)
                .andIf(!includeInactive, "m.active = 1")
                .andIf("low".equals(stock), "m.active = 1 AND COALESCE(s.available, 0) <= m.reorder_level")
                .andIf("out".equals(stock), "m.active = 1 AND COALESCE(s.available, 0) = 0");
        return page(con, SELECT, FROM, where, request.orderBy(SORTS, "m.name ASC"), request);
    }

    public List<Medicine> findAllActive(Connection con) throws SQLException {
        LocalDate today = LocalDate.now();
        return query(con, SELECT + " " + FROM + " WHERE m.active = 1 ORDER BY m.name", today, today);
    }

    public List<String> findCategories(Connection con) throws SQLException {
        return query(con, "SELECT DISTINCT category FROM medicines ORDER BY category", rs -> rs.getString(1));
    }

    public boolean existsSame(Connection con, Medicine m) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM medicines WHERE LOWER(name) = LOWER(?) AND COALESCE(strength,'') = ? "
                        + "AND LOWER(dosage_form) = LOWER(?) AND id <> ?",
                m.getName(), m.getStrength() == null ? "" : m.getStrength(), m.getDosageForm(),
                m.getId() == null ? -1L : m.getId()) > 0;
    }

    public long countActive(Connection con) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM medicines WHERE active = 1");
    }

    public long countLowStock(Connection con, LocalDate today) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) " + FROM + " WHERE m.active = 1 AND COALESCE(s.available, 0) <= m.reorder_level",
                today, today);
    }
}
