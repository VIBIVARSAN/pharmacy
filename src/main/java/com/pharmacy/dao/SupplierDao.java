package com.pharmacy.dao;

import com.pharmacy.model.Supplier;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SupplierDao extends AbstractDao<Supplier> implements CrudDao<Supplier> {
    private static final String SELECT = "SELECT id, supplier_code, name, contact_person, phone, email, address, active, "
            + "created_at, updated_at";
    private static final Map<String, String> SORTS = Map.of("code", "supplier_code", "name", "name",
            "contact", "contact_person");

    @Override
    protected Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getLong("id"));
        s.setSupplierCode(rs.getString("supplier_code"));
        s.setName(rs.getString("name"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email"));
        s.setAddress(rs.getString("address"));
        s.setActive(rs.getBoolean("active"));
        s.setCreatedAt(dateTime(rs, "created_at"));
        s.setUpdatedAt(dateTime(rs, "updated_at"));
        return s;
    }

    @Override
    public Optional<Supplier> findById(Connection con, long id) throws SQLException {
        return queryOne(con, SELECT + " FROM suppliers WHERE id = ?", id);
    }

    @Override
    public long insert(Connection con, Supplier s) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO suppliers (supplier_code, name, contact_person, phone, email, address, active, created_at) VALUES (?,?,?,?,?,?,?,?)",
                s.getSupplierCode(), s.getName(), s.getContactPerson(), s.getPhone(), s.getEmail(), s.getAddress(),
                s.isActive(), now());
    }

    @Override
    public void update(Connection con, Supplier s) throws SQLException {
        update(con, "UPDATE suppliers SET name = ?, contact_person = ?, phone = ?, email = ?, address = ? WHERE id = ?",
                s.getName(), s.getContactPerson(), s.getPhone(), s.getEmail(), s.getAddress(), s.getId());
    }

    public void setActive(Connection con, long id, boolean active) throws SQLException {
        update(con, "UPDATE suppliers SET active = ? WHERE id = ?", active, id);
    }

    public Page<Supplier> search(Connection con, PageRequest request, boolean includeInactive) throws SQLException {
        Where where = new Where().search(request.getQuery(), "supplier_code", "name", "contact_person", "phone")
                .andIf(!includeInactive, "active = 1");
        return page(con, SELECT, "FROM suppliers", where, request.orderBy(SORTS, "name ASC"), request);
    }

    public List<Supplier> findAllActive(Connection con) throws SQLException {
        return query(con, SELECT + " FROM suppliers WHERE active = 1 ORDER BY name");
    }
}
