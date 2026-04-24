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

    private static String getProperty(String key, String defaultValue) {
        // 1. Check Java System Property (for tests) - Has highest priority for tests
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // 2. Check Environment Variable
        value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }

        // 3. Check properties file
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("server/src/main/resources/application.properties")) {
            props.load(fis);
            value = props.getProperty(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        } catch (IOException e) {
            logger.debug("Properties file not found, using default values");
        }

        return defaultValue;
    }

    public static synchronized HikariDataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();

            String host = getProperty("DB_HOST", "localhost");
            String port = getProperty("DB_PORT", "5432");
            String name = getProperty("DB_NAME", "jobdb");
            String user = getProperty("DB_USER", "jobuser");
            String password = getProperty("DB_PASSWORD", "secret");

            config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", host, port, name));
            config.setUsername(user);
            config.setPassword(password);

            int maxPoolSize = Integer.parseInt(getProperty("DB_MAX_POOL_SIZE", "10"));
            int minIdle = Integer.parseInt(getProperty("DB_MIN_IDLE", "2"));
            long connectionTimeout = Long.parseLong(getProperty("DB_CONNECTION_TIMEOUT", "30000"));
            long idleTimeout = Long.parseLong(getProperty("DB_IDLE_TIMEOUT", "600000"));
            long maxLifetime = Long.parseLong(getProperty("DB_MAX_LIFETIME", "1800000"));

            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);
            config.setConnectionTimeout(connectionTimeout);
            config.setIdleTimeout(idleTimeout);
            config.setMaxLifetime(maxLifetime);

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

    public static void reset() {
        close();
        dataSource = null;
        logger.info("HikariCP connection pool reset");
    }

    public static void init() {
        reset();
        getDataSource();
    }
}