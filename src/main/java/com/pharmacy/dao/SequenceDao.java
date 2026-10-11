package com.pharmacy.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Hands out gap-tolerant, never-repeating numbers used to build readable IDs.
 * The UPDATE takes a row lock, so two concurrent transactions can never
 * receive the same number.
 */
public class SequenceDao extends AbstractDao<Long> {

    @Override
    protected Long mapRow(ResultSet rs) throws SQLException {
        return rs.getLong(1);
    }

    public long next(Connection con, String sequenceName) throws SQLException {
        int rows = update(con, "UPDATE sequences SET next_val = LAST_INSERT_ID(next_val + 1) WHERE name = ?", sequenceName);
        if (rows != 1) {
            throw new SQLException("Unknown sequence " + sequenceName + ". Was database.sql imported?");
        }
        return queryLong(con, "SELECT LAST_INSERT_ID()");
    }

    /** e.g. nextCode(con, "PATIENT", "PAT") returns "PAT-000042". */
    public String nextCode(Connection con, String sequenceName, String prefix) throws SQLException {
        return String.format("%s-%06d", prefix, next(con, sequenceName));
    }
}
