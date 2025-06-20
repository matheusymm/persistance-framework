package com.persistence;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.persistence.annotation.Column;
import com.persistence.annotation.Entity;
import com.persistence.db.DbConnection;
import com.persistence.utils.ColumnHelper;

public class PersistenceFramework {
    private SchemaGenerator schemaGenerator;
    private Class<?> entityClass;
    private String tableName;
    private ColumnHelper columnHelper;

    public PersistenceFramework(Class<?> entityClass) {
        this.entityClass = entityClass;
        this.schemaGenerator = new SchemaGenerator();
        this.columnHelper = new ColumnHelper();
        initializeEntityMetadata();
    }

    private void initializeEntityMetadata() {
        if (!entityClass.isAnnotationPresent(Entity.class)) {
            throw new IllegalArgumentException("Class " + entityClass.getName() + " is not annotated with @Entity");
        }
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        this.tableName = entityAnnotation.name();
    }

    public void initializeSchema() {
        try {
            if (!entityClass.isAnnotationPresent(Entity.class)) {
                throw new IllegalArgumentException("Provided class is not an entity: " + entityClass.getName());
            }
            System.out.println("Generating schema for table: " + tableName);
            schemaGenerator.generateSchema(entityClass);
            System.out.println("Schema generation complete for: " + entityClass.getSimpleName());
        } catch (SQLException e) {
            System.err.println("Error generating schema for " + entityClass.getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void insert(Object object) throws IllegalAccessException, SQLException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet generatedKeys = null;
        try {
            if (!this.entityClass.isInstance(object)) {
                throw new IllegalArgumentException("Object of type " + object.getClass().getName() +
                        " does not match configured entity class " + this.entityClass.getName());
            }

            connection = DbConnection.getDbConnection();

            List<Field> fieldsForInsert = Arrays.stream(entityClass.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Column.class))
                    .peek(field -> field.setAccessible(true))
                    .collect(Collectors.toList());

            String columns = fieldsForInsert.stream()
                    .map(columnHelper::getColumnName)
                    .collect(Collectors.joining(", "));

            String placeholders = fieldsForInsert.stream()
                    .map(f -> "?")
                    .collect(Collectors.joining(", "));

            String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
            System.out.println("Generated INSERT SQL: " + sql);
            preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            int paramIndex = 1;
            for (Field field : fieldsForInsert) {
                preparedStatement.setObject(paramIndex++, field.get(object));
            }

            preparedStatement.executeUpdate();
            System.out.println("Insert operation executed for " + entityClass.getSimpleName() + ".");

            generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                System.out.println("Generated key(s) found after insert:");
                for (int i = 1; i <= generatedKeys.getMetaData().getColumnCount(); i++) {
                    System.out.println("  Column " + generatedKeys.getMetaData().getColumnName(i) + ": "
                            + generatedKeys.getObject(i));
                }
            } else {
                System.out.println("No generated keys found after insert for " + entityClass.getSimpleName() + ".");
            }
        } finally {
            connection.close();
        }
    }
}
