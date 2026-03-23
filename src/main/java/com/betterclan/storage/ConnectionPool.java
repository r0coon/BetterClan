package com.betterclan.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public final class ConnectionPool {

    private final HikariDataSource ds;
    private final Dialect dialect;
    private final Logger logger;

    public ConnectionPool(Dialect dialect, String host, int port, String database,
                           String user, String password, int poolSize, Logger logger) {
        this.dialect = dialect;
        this.logger = logger;

        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName(dialect.driverClass());
        cfg.setJdbcUrl(dialect.urlPrefix() + host + ":" + port + "/" + database);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(poolSize);
        cfg.setMinimumIdle(2);
        cfg.setIdleTimeout(30_000);
        cfg.setMaxLifetime(600_000);
        cfg.setConnectionTimeout(10_000);
        cfg.setPoolName("BetterClan-DB");

        if (dialect != Dialect.POSTGRES) {
            cfg.addDataSourceProperty("useSSL", "false");
            cfg.addDataSourceProperty("characterEncoding", "utf8mb4");
        }

        this.ds = new HikariDataSource(cfg);
        logger.info("DB-Pool gestartet (" + dialect.name() + ", pool=" + poolSize + ")");
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public Dialect getDialect() { return dialect; }

    public void shutdown() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
            logger.info("DB-Pool geschlossen.");
        }
    }
}

