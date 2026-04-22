package com.jobapp.server.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    private static final String DB_HOST = getProperty("DB_HOST", "localhost");
    private static final String DB_PORT = getProperty("DB_PORT", "5432");
    private static final String DB_NAME = getProperty("DB_NAME", "jobdb");
    private static final String DB_USER = getProperty("DB_USER", "jobuser");
    private static final String DB_PASSWORD = getProperty("DB_PASSWORD", "secret");

    private static final int MAX_POOL_SIZE = Integer.parseInt(getProperty("DB_MAX_POOL_SIZE", "10"));
    private static final int MIN_IDLE = Integer.parseInt(getProperty("DB_MIN_IDLE", "2"));
    private static final long CONNECTION_TIMEOUT = Long.parseLong(getProperty("DB_CONNECTION_TIMEOUT", "30000"));
    private static final long IDLE_TIMEOUT = Long.parseLong(getProperty("DB_IDLE_TIMEOUT", "600000"));
    private static final long MAX_LIFETIME = Long.parseLong(getProperty("DB_MAX_LIFETIME", "1800000"));

    private static String getProperty(String key, String defaultValue) {
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("server/src/main/resources/application.properties")) {
            props.load(fis);
            String propValue = props.getProperty(key);
            if (propValue != null && !propValue.isEmpty()) {
                return propValue;
            }
        } catch (IOException e) {
            logger.debug("Properties file not found, using default values");
        }

        return defaultValue;
    }

    public static HikariDataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME));
            config.setUsername(DB_USER);
            config.setPassword(DB_PASSWORD);

            config.setMaximumPoolSize(MAX_POOL_SIZE);
            config.setMinimumIdle(MIN_IDLE);
            config.setConnectionTimeout(CONNECTION_TIMEOUT);
            config.setIdleTimeout(IDLE_TIMEOUT);
            config.setMaxLifetime(MAX_LIFETIME);

            config.setPoolName("JobFinder-HikariCP");
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            logger.info("HikariCP connection pool initialized: {}", config.getPoolName());
            logger.debug("JDBC URL: {}", config.getJdbcUrl());
        }
        return dataSource;
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("HikariCP connection pool closed");
        }
    }
}