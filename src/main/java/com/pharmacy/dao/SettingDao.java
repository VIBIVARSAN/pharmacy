package com.pharmacy.dao;

import com.pharmacy.model.Setting;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SettingDao extends AbstractDao<Setting> {

    @Override
    protected Setting mapRow(ResultSet rs) throws SQLException {
        return new Setting(rs.getString("setting_key"), rs.getString("setting_value"), rs.getString("description"));
    }

    public List<Setting> findAll(Connection con) throws SQLException {
        return query(con, "SELECT setting_key, setting_value, description FROM settings ORDER BY setting_key");
    }

    public int updateValue(Connection con, String key, String value) throws SQLException {
        return update(con, "UPDATE settings SET setting_value = ? WHERE setting_key = ?", value, key);
    }
}
