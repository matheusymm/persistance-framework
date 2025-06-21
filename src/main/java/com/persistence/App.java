package com.persistence;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import com.persistence.model.User;

public class App {
    public static void main(String[] args) {
        PersistenceFramework persistenceFramework = new PersistenceFramework(User.class);
        persistenceFramework.initializeSchema();

        try {
            User u1 = new User(1, "User 1", "user1@example.com");
            User u2 = new User(2, "User 2", "user2@example.com");

            persistenceFramework.insert(u1);
            persistenceFramework.insert(u2);
            System.out.println("Inserted users: " + persistenceFramework.findAll());

            u1.setName("User 1 Updated");
            persistenceFramework.update(u1);
            System.out.println("Updated user: " + persistenceFramework.findById(1));

            persistenceFramework.delete(u2);
            System.out.println("Deleted user 2. Remaining users: " + persistenceFramework.findAll());
        } catch (IllegalAccessException | SQLException | SecurityException | NoSuchFieldException
                | InstantiationException | NoSuchMethodException | IllegalArgumentException
                | InvocationTargetException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            persistenceFramework.close();
        }
    }
}
