package com.persistence;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.persistence.annotation.Column;
import com.persistence.annotation.Entity;
import com.persistence.db.DbConnection;
import com.persistence.utils.ColumnHelper;

public class SchemaGenerator {
    private static final Map<Class<?>, String> JAVA_TO_SQL_TYPE_MAP = new HashMap<>();
    private final ColumnHelper columnHelper;
    private DbConnection dbConnection;

    static {
        JAVA_TO_SQL_TYPE_MAP.put(String.class, "VARCHAR(255)");
        JAVA_TO_SQL_TYPE_MAP.put(Long.class, "BIGINT");
        JAVA_TO_SQL_TYPE_MAP.put(long.class, "BIGINT");
        JAVA_TO_SQL_TYPE_MAP.put(Integer.class, "INTEGER");
        JAVA_TO_SQL_TYPE_MAP.put(int.class, "INTEGER");
        JAVA_TO_SQL_TYPE_MAP.put(Boolean.class, "BOOLEAN");
        JAVA_TO_SQL_TYPE_MAP.put(boolean.class, "BOOLEAN");
        JAVA_TO_SQL_TYPE_MAP.put(Double.class, "DOUBLE PRECISION");
        JAVA_TO_SQL_TYPE_MAP.put(double.class, "DOUBLE PRECISION");
        JAVA_TO_SQL_TYPE_MAP.put(Float.class, "REAL");
        JAVA_TO_SQL_TYPE_MAP.put(float.class, "REAL");
        JAVA_TO_SQL_TYPE_MAP.put(java.util.Date.class, "TIMESTAMP");
        JAVA_TO_SQL_TYPE_MAP.put(Date.class, "DATE");
        JAVA_TO_SQL_TYPE_MAP.put(Timestamp.class, "TIMESTAMP");
    }

    public SchemaGenerator() {
        this.columnHelper = new ColumnHelper();
        this.dbConnection = DbConnection.getDbConnection();
    }

    public void generateSchema(Class<?>... entityClasses) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dbConnection.getConnection();
            stmt = conn.createStatement();
            for (Class<?> entityClass : entityClasses) {
                if (entityClass.isAnnotationPresent(Entity.class)) {
                    String sql = createTableSql(entityClass);
                    stmt.execute(sql);
                    System.out.println("Generating schema for table: " + entityClass.getSimpleName());
                } else {
                    System.err.println("Warning: Class " + entityClass.getName()
                            + " is not an @Entity and will be skipped for schema generation.");
                }
            }
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private String createTableSql(Class<?> entityClass) {
        Entity entityAnnotation = entityClass.getAnnotation(Entity.class);
        String tableName = entityAnnotation.name();

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");

        List<Field> fields = Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .collect(Collectors.toList());

        for (Field field : fields) {
            String columnName = columnHelper.getColumnName(field);
            String columnType = getSqlType(field.getType());

            sqlBuilder.append(columnName).append(" ").append(columnType);

            Column columnAnnotation = field.getAnnotation(Column.class);
            if (columnAnnotation != null) {
                if (!columnAnnotation.nullable()) {
                    sqlBuilder.append(" NOT NULL");
                }
                if (columnAnnotation.unique()) {
                    sqlBuilder.append(" UNIQUE");
                }
                if (columnAnnotation.primaryKey()) {
                    sqlBuilder.append(" PRIMARY KEY");
                }
            }
            sqlBuilder.append(", ");
        }

        if (fields.isEmpty()) {
            sqlBuilder.append("dummy_id BIGSERIAL PRIMARY KEY");
        } else {
            sqlBuilder.setLength(sqlBuilder.length() - 2);
        }

        sqlBuilder.append(");");
        return sqlBuilder.toString();
    }

    private String getSqlType(Class<?> javaType) {
        return JAVA_TO_SQL_TYPE_MAP.getOrDefault(javaType, "TEXT");
    }
}