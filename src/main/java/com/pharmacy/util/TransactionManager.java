package com.pharmacy.util;

import com.pharmacy.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Central place for the commit / rollback strategy.
 * <ul>
 *   <li>{@link #inTransaction} - all statements commit together or not at all.</li>
 *   <li>{@link #read} - read-only work on an auto-commit connection.</li>
 * </ul>
 * Connections are always returned to the pool by try-with-resources.
 */
public class TransactionManager {
    private final ConnectionProvider provider;

    public TransactionManager(ConnectionProvider provider) {
        this.provider = provider;
    }

    public <T> T inTransaction(TransactionCallback<T> work) {
        try (Connection con = provider.getConnection()) {
            con.setAutoCommit(false);
            try {
                T result = work.execute(con);
                con.commit();
                return result;
            } catch (SQLException | RuntimeException | Error e) {
                rollbackQuietly(con, e);
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("A database error occurred.", e);
        }
    }

    public <T> T read(TransactionCallback<T> work) {
        try (Connection con = provider.getConnection()) {
            return work.execute(con);
        } catch (SQLException e) {
            throw new DataAccessException("A database error occurred.", e);
        }
    }

    private static void rollbackQuietly(Connection con, Throwable original) {
        try {
            con.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }
}
