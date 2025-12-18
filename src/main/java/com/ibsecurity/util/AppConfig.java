package com.ibsecurity.util;

/**
 * Конфигурация приложения.
 */
public class AppConfig {
    
    // Параметры базы данных
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "5432";
    private static final String DB_NAME = "ib_employees";
    private static final String DB_USER = "ib_admin";
    private static final String DB_PASSWORD = "ib_secure_pass_2024";
    
    public static String getDbHost() {
        return DB_HOST;
    }
    
    public static String getDbPort() {
        return DB_PORT;
    }
    
    public static String getDbName() {
        return DB_NAME;
    }
    
    public static String getDbUser() {
        return DB_USER;
    }
    
    public static String getDbPassword() {
        return DB_PASSWORD;
    }
    
    public static String getDbUrl() {
        return String.format("jdbc:postgresql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);
    }
}
