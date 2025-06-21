package com.persistence.db;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnection {
    private static final String db_url = "jdbc:postgresql://localhost:5432/framework_db";
    private static final String db_user = "user";
    private static final String db_password = "pass";
    private static DbConnection instance;
    private Connection connection;

    private DbConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection(db_url, db_user, db_password);
        } catch (Exception e) {
            throw new RuntimeException("Error connecting to the database", e);
        }
    }

    public static DbConnection getDbConnection() {
        if (instance == null) {
            instance = new DbConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database connection is not initialized.");
        }
        return connection;
    }

    public static synchronized void closeDbConnection() {
        if (instance == null || instance.connection == null) {
            return;
        }

        try {
            instance.connection.close();
        } catch (Exception e) {
            throw new RuntimeException("Error closing the database connection", e);
        } finally {
            instance = null;
            System.out.println("Database connection closed.");
        }
    }
}
