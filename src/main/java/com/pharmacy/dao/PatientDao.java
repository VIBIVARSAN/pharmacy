package com.pharmacy.dao;

import com.pharmacy.model.Gender;
import com.pharmacy.model.Patient;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PatientDao extends AbstractDao<Patient> implements CrudDao<Patient> {
    private static final String SELECT = "SELECT id, patient_code, full_name, date_of_birth, gender, phone, email, "
            + "address, active, created_at, updated_at";
    private static final Map<String, String> SORTS = Map.of("code", "patient_code", "name", "full_name",
            "phone", "phone", "registered", "created_at");

    @Override
    protected Patient mapRow(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getLong("id"));
        p.setPatientCode(rs.getString("patient_code"));
        p.setFullName(rs.getString("full_name"));
        p.setDateOfBirth(date(rs, "date_of_birth"));
        p.setGender(Gender.valueOf(rs.getString("gender")));
        p.setPhone(rs.getString("phone"));
        p.setEmail(rs.getString("email"));
        p.setAddress(rs.getString("address"));
        p.setActive(rs.getBoolean("active"));
        p.setCreatedAt(dateTime(rs, "created_at"));
        p.setUpdatedAt(dateTime(rs, "updated_at"));
        return p;
    }

    @Override
    public Optional<Patient> findById(Connection con, long id) throws SQLException {
        return queryOne(con, SELECT + " FROM patients WHERE id = ?", id);
    }

    @Override
    public long insert(Connection con, Patient p) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO patients (patient_code, full_name, date_of_birth, gender, phone, email, address, active, created_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?)",
                p.getPatientCode(), p.getFullName(), p.getDateOfBirth(), p.getGender(), p.getPhone(), p.getEmail(),
                p.getAddress(), p.isActive(), now());
    }

    @Override
    public void update(Connection con, Patient p) throws SQLException {
        update(con, "UPDATE patients SET full_name = ?, date_of_birth = ?, gender = ?, phone = ?, email = ?, address = ? WHERE id = ?",
                p.getFullName(), p.getDateOfBirth(), p.getGender(), p.getPhone(), p.getEmail(), p.getAddress(), p.getId());
    }

    public void setActive(Connection con, long id, boolean active) throws SQLException {
        update(con, "UPDATE patients SET active = ? WHERE id = ?", active, id);
    }

    /** A patient with the same name and phone is treated as a probable duplicate. */
    public Optional<Patient> findDuplicate(Connection con, String fullName, String phone, Long excludeId) throws SQLException {
        return queryOne(con, SELECT + " FROM patients WHERE LOWER(full_name) = LOWER(?) AND phone = ? AND id <> ? LIMIT 1",
                fullName, phone, excludeId == null ? -1L : excludeId);
    }

    public Page<Patient> search(Connection con, PageRequest request, boolean includeInactive) throws SQLException {
        Where where = new Where().search(request.getQuery(), "patient_code", "full_name", "phone")
                .andIf(!includeInactive, "active = 1");
        return page(con, SELECT, "FROM patients", where, request.orderBy(SORTS, "created_at DESC, id DESC"), request);
    }

    /** Short list of active patients for the autocomplete lookup. */
    public List<Patient> lookup(Connection con, String term, int limit) throws SQLException {
        Where where = new Where().and("active = 1").search(term, "patient_code", "full_name", "phone");
        Object[] params = where.params().toArray(new Object[where.params().size() + 1]);
        params[params.length - 1] = limit;
        return query(con, SELECT + " FROM patients" + where.sql() + " ORDER BY full_name LIMIT ?", params);
    }

    public long countAll(Connection con) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM patients WHERE active = 1");
    }
}
