package com.pharmacy.dao;

import com.pharmacy.model.Doctor;
import com.pharmacy.util.Page;
import com.pharmacy.util.PageRequest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DoctorDao extends AbstractDao<Doctor> implements CrudDao<Doctor> {
    private static final String SELECT = "SELECT id, doctor_code, full_name, specialization, phone, email, hospital, "
            + "registration_no, active, created_at, updated_at";
    private static final Map<String, String> SORTS = Map.of("code", "doctor_code", "name", "full_name",
            "specialization", "specialization", "hospital", "hospital");

    @Override
    protected Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor d = new Doctor();
        d.setId(rs.getLong("id"));
        d.setDoctorCode(rs.getString("doctor_code"));
        d.setFullName(rs.getString("full_name"));
        d.setSpecialization(rs.getString("specialization"));
        d.setPhone(rs.getString("phone"));
        d.setEmail(rs.getString("email"));
        d.setHospital(rs.getString("hospital"));
        d.setRegistrationNo(rs.getString("registration_no"));
        d.setActive(rs.getBoolean("active"));
        d.setCreatedAt(dateTime(rs, "created_at"));
        d.setUpdatedAt(dateTime(rs, "updated_at"));
        return d;
    }

    @Override
    public Optional<Doctor> findById(Connection con, long id) throws SQLException {
        return queryOne(con, SELECT + " FROM doctors WHERE id = ?", id);
    }

    @Override
    public long insert(Connection con, Doctor d) throws SQLException {
        return insertAndReturnKey(con,
                "INSERT INTO doctors (doctor_code, full_name, specialization, phone, email, hospital, registration_no, active, created_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?)",
                d.getDoctorCode(), d.getFullName(), d.getSpecialization(), d.getPhone(), d.getEmail(), d.getHospital(),
                d.getRegistrationNo(), d.isActive(), now());
    }

    @Override
    public void update(Connection con, Doctor d) throws SQLException {
        update(con, "UPDATE doctors SET full_name = ?, specialization = ?, phone = ?, email = ?, hospital = ?, registration_no = ? WHERE id = ?",
                d.getFullName(), d.getSpecialization(), d.getPhone(), d.getEmail(), d.getHospital(), d.getRegistrationNo(), d.getId());
    }

    public void setActive(Connection con, long id, boolean active) throws SQLException {
        update(con, "UPDATE doctors SET active = ? WHERE id = ?", active, id);
    }

    public boolean registrationNoExists(Connection con, String registrationNo, Long excludeId) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM doctors WHERE registration_no = ? AND id <> ?",
                registrationNo, excludeId == null ? -1L : excludeId) > 0;
    }

    public Page<Doctor> search(Connection con, PageRequest request, boolean includeInactive) throws SQLException {
        Where where = new Where().search(request.getQuery(), "doctor_code", "full_name", "specialization", "hospital", "phone")
                .andIf(!includeInactive, "active = 1");
        return page(con, SELECT, "FROM doctors", where, request.orderBy(SORTS, "full_name ASC"), request);
    }

    public List<Doctor> lookup(Connection con, String term, int limit) throws SQLException {
        Where where = new Where().and("active = 1").search(term, "doctor_code", "full_name", "specialization");
        Object[] params = where.params().toArray(new Object[where.params().size() + 1]);
        params[params.length - 1] = limit;
        return query(con, SELECT + " FROM doctors" + where.sql() + " ORDER BY full_name LIMIT ?", params);
    }

    public long countAll(Connection con) throws SQLException {
        return queryLong(con, "SELECT COUNT(*) FROM doctors WHERE active = 1");
    }
}
