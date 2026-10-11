package com.pharmacy.exception;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

/** Unchecked wrapper around SQLException so JDBC details do not leak into the web layer. */
public class DataAccessException extends AppException {
    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, SQLException cause) {
        super(message, cause);
    }

    public SQLException getSqlException() {
        return (SQLException) getCause();
    }

    /** True for unique-key / foreign-key violations (SQLState class 23). */
    public boolean isConstraintViolation() {
        SQLException e = getSqlException();
        return e instanceof SQLIntegrityConstraintViolationException
                || (e.getSQLState() != null && e.getSQLState().startsWith("23"));
    }

    /** True for a deadlock or lock-wait timeout: the transaction was rolled back and may simply be retried. */
    public boolean isTransient() {
        SQLException e = getSqlException();
        return "40001".equals(e.getSQLState()) || e.getErrorCode() == 1213 || e.getErrorCode() == 1205;
    }

    /** True when a UNIQUE key rejected the row (MySQL error 1062). */
    public boolean isDuplicateKey() {
        return isConstraintViolation() && getSqlException().getErrorCode() == 1062;
    }
}
