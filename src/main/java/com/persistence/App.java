package com.persistence;

import java.sql.SQLException;

import com.persistence.model.User;

public class App {
    public static void main(String[] args) {
        PersistenceFramework persistenceFramework = new PersistenceFramework(User.class);
        persistenceFramework.initializeSchema();

        User user = new User(1, "John", "jhon@example.com");

        try {
            persistenceFramework.insert(user);
        } catch (IllegalAccessException | SQLException e) {
            System.err.println("Error inserting user: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
