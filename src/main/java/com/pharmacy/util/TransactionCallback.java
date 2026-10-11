package com.pharmacy.util;

import java.sql.Connection;
import java.sql.SQLException;

/** A unit of JDBC work that returns a value. */
@FunctionalInterface
public interface TransactionCallback<T> {
    T execute(Connection connection) throws SQLException;
}
