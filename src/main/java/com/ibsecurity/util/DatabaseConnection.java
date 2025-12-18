package com.ibsecurity.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Управление подключением к базе данных.
 */
public class DatabaseConnection {
    
    private static HikariDataSource dataSource;

    static {
        initializeDataSource();
    }
    
    private static void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(AppConfig.getDbUrl());
        config.setUsername(AppConfig.getDbUser());
        config.setPassword(AppConfig.getDbPassword());
        
        // Параметры пула соединений
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        config.setPoolName("IB-HikariPool");
        config.setAutoCommit(true);
        
        dataSource = new HikariDataSource(config);
    }

    /**
     * Получение соединения из пула.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Закрытие пула соединений.
     */
    public static void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
