package com.persistence.db;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnection {
    private static DbConnection instance;
    private Connection connection;

    private DbConnection() {
        try {
            // Carregar propriedades do arquivo application.properties
            Properties properties = new Properties();
            properties.load(new FileInputStream("src/main/resources/application.properties"));

            String dbUrl = properties.getProperty("db.url");
            String dbUser = properties.getProperty("db.user");
            String dbPassword = properties.getProperty("db.password");
            String dbDriver = properties.getProperty("db.driver");

            // Inicializar conexão com o banco de dados
            Class.forName(dbDriver);
            this.connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (IOException e) {
            throw new RuntimeException("Error loading database properties", e);
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
