package com.pharmacy.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/** ConnectionProvider backed by a HikariCP JDBC connection pool (a javax.sql.DataSource). */
public class PooledConnectionProvider implements ConnectionProvider {
    private final HikariDataSource dataSource;

    public PooledConnectionProvider(String url, String user, String password, String driver, int poolSize) {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("pharmacy-pool");
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        if (driver != null && !driver.isBlank()) {
            cfg.setDriverClassName(driver);
        }
        cfg.setMaximumPoolSize(poolSize);
        cfg.setMinimumIdle(Math.min(2, poolSize));
        cfg.setConnectionTimeout(10_000);
        // Do not fail application start-up when MySQL is down; report it on first use instead.
        cfg.setInitializationFailTimeout(-1);
        this.dataSource = new HikariDataSource(cfg);
    }

    /** Builds the pool from DB_URL / DB_USER / DB_PASSWORD / DB_DRIVER / DB_POOL_SIZE. */
    public static PooledConnectionProvider fromConfig() {
        String url = AppConfig.get("DB_URL");
        String user = AppConfig.get("DB_USER");
        if (url == null || user == null) {
            throw new IllegalStateException("Database is not configured. Set DB_URL, DB_USER and DB_PASSWORD "
                    + "as environment variables or in src/main/resources/db.properties (see README).");
        }
        return new PooledConnectionProvider(url, user, AppConfig.get("DB_PASSWORD", ""),
                AppConfig.get("DB_DRIVER", "com.mysql.cj.jdbc.Driver"), AppConfig.getInt("DB_POOL_SIZE", 10));
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
