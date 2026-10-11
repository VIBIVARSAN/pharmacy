package com.pharmacy.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Converts the current row of a ResultSet into an object. */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
}
