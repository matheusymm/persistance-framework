package com.persistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import com.persistence.annotation.Entity;
import com.persistence.db.DbConnection;
import com.persistence.utils.ColumnHelper;

public class PersistenceFramework {
    private SchemaGenerator schemaGenerator;
    private Class<?> entityClass;
    private String tableName;
    private ColumnHelper columnHelper;
    private DbConnection dbConnection;

    public PersistenceFramework(Class<?> entityClass) {
        this.entityClass = entityClass;
        this.schemaGenerator = new SchemaGenerator();
        this.columnHelper = new ColumnHelper();
        this.dbConnection = DbConnection.getDbConnection();
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
                throw new IllegalArgumentException("Provided class is not an entity: " + tableName);
            }
            schemaGenerator.generateSchema(entityClass);
        } catch (SQLException e) {
            System.err.println("Error generating schema for " + tableName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void insert(Object object) throws IllegalAccessException, SQLException {
        Connection conn = null;
        PreparedStatement pStmt = null;
        try {
            if (!this.entityClass.isInstance(object)) {
                throw new IllegalArgumentException("Object of type " + object.getClass().getName() +
                        " does not match configured entity class " + tableName);
            }

            conn = dbConnection.getConnection();

            List<Field> fieldsForInsert = columnHelper.getAnnotatedFields(entityClass);

            String columns = fieldsForInsert.stream()
                    .map(columnHelper::getColumnName)
                    .collect(Collectors.joining(", "));

            String placeholders = fieldsForInsert.stream()
                    .map(f -> "?")
                    .collect(Collectors.joining(", "));

            String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
            pStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            int paramIndex = 1;
            for (Field field : fieldsForInsert) {
                pStmt.setObject(paramIndex++, field.get(object));
            }

            pStmt.executeUpdate();
        } finally {
            if (pStmt != null)
                pStmt.close();
        }
    }

    public Object findById(Object id)
            throws SQLException, IllegalAccessException, InstantiationException, NoSuchFieldException,
            NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {
        Connection conn = null;
        PreparedStatement pStmt = null;
        ResultSet resultSet = null;
        try {
            conn = dbConnection.getConnection();

            Field pkField = columnHelper.getPrimaryKeyField(entityClass);
            String pkColumn = columnHelper.getColumnName(pkField);

            String sql = "SELECT * FROM " + tableName + " WHERE " + pkColumn + " = ?";
            pStmt = conn.prepareStatement(sql);
            pStmt.setObject(1, id);

            resultSet = pStmt.executeQuery();
            if (resultSet.next()) {
                Constructor<?> constructor = entityClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object entityInstance = constructor.newInstance();
                for (Field field : columnHelper.getAnnotatedFields(entityClass)) {
                    field.setAccessible(true);
                    field.set(entityInstance, resultSet.getObject(columnHelper.getColumnName(field)));
                }
                return entityInstance;
            } else {
                System.out.println("No record found with id: " + id);
                return null;
            }
        } finally {
            if (resultSet != null)
                resultSet.close();
            if (pStmt != null)
                pStmt.close();
        }
    }

    public List<Object> findAll() throws SQLException, IllegalAccessException, InstantiationException,
            NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {
        Connection conn = null;
        PreparedStatement pStmt = null;
        ResultSet resultSet = null;
        try {
            conn = dbConnection.getConnection();

            String sql = "SELECT * FROM " + tableName;
            pStmt = conn.prepareStatement(sql);

            resultSet = pStmt.executeQuery();
            List<Object> entities = new java.util.ArrayList<>();
            while (resultSet.next()) {
                Constructor<?> constructor = entityClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object entityInstance = constructor.newInstance();
                for (Field field : columnHelper.getAnnotatedFields(entityClass)) {
                    field.setAccessible(true);
                    field.set(entityInstance, resultSet.getObject(columnHelper.getColumnName(field)));
                }
                entities.add(entityInstance);
            }
            return entities;
        } finally {
            if (resultSet != null)
                resultSet.close();
            if (pStmt != null)
                pStmt.close();
        }
    }

    public void update(Object object)
            throws IllegalAccessException, SQLException, NoSuchFieldException, SecurityException {
        Connection conn = null;
        PreparedStatement pStmt = null;
        try {
            if (!this.entityClass.isInstance(object)) {
                throw new IllegalArgumentException("Object of type " + object.getClass().getName() +
                        " does not match configured entity class " + tableName);
            }

            conn = dbConnection.getConnection();

            List<Field> fieldsForUpdate = columnHelper.getAnnotatedFields(entityClass);

            Field pkField = columnHelper.getPrimaryKeyField(entityClass);
            String pkColumn = columnHelper.getColumnName(pkField);

            List<Field> nonPkFields = fieldsForUpdate.stream()
                    .filter(f -> !f.equals(pkField))
                    .collect(Collectors.toList());

            String setClause = nonPkFields.stream()
                    .map(field -> columnHelper.getColumnName(field) + " = ?")
                    .collect(Collectors.joining(", "));

            String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + pkColumn + " = ?";
            pStmt = conn.prepareStatement(sql);

            int paramIndex = 1;
            for (Field field : nonPkFields) {
                pStmt.setObject(paramIndex++, field.get(object));
            }
            pStmt.setObject(paramIndex, pkField.get(object));
        } finally {
            if (pStmt != null)
                pStmt.close();
        }
    }

    public void delete(Object object)
            throws IllegalAccessException, SQLException, NoSuchFieldException, SecurityException {
        Connection conn = null;
        PreparedStatement pStmt = null;
        try {
            if (!this.entityClass.isInstance(object)) {
                throw new IllegalArgumentException("Object of type " + object.getClass().getName() +
                        " does not match configured entity class " + tableName);
            }

            conn = dbConnection.getConnection();

            Field pkField = columnHelper.getPrimaryKeyField(entityClass);
            String pkColumn = columnHelper.getColumnName(pkField);
            Object pkValue = pkField.get(object);

            String sql = "DELETE FROM " + tableName + " WHERE " + pkColumn + " = ?";
            pStmt = conn.prepareStatement(sql);
            pStmt.setObject(1, pkValue);
        } finally {
            if (pStmt != null)
                pStmt.close();
        }
    }

    public void close() {
        DbConnection.closeDbConnection();
    }
}
