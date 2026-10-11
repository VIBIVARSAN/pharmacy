package com.pharmacy.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Abstraction over "where JDBC connections come from". The web application
 * uses a pooled implementation; tests can plug in a simple one.
 */
public interface ConnectionProvider {
    Connection getConnection() throws SQLException;

    /** Releases any resources held by the provider. */
    default void close() {
    }
}
