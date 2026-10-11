package com.pharmacy.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Contract shared by the DAOs of the main master-data entities.
 * The Connection is passed in by the service layer so that several DAO calls
 * can take part in one database transaction.
 */
public interface CrudDao<T> {
    Optional<T> findById(Connection con, long id) throws SQLException;

    /** Inserts the entity and returns the generated primary key. */
    long insert(Connection con, T entity) throws SQLException;

    void update(Connection con, T entity) throws SQLException;
}
