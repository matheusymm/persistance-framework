package com.persistence.utils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.persistence.annotation.Column;

public class ColumnHelper {
    public String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            return columnAnnotation.name();
        }
        return field.getName().toLowerCase();
    }

    public List<Field> getAnnotatedFields(Class<?> entityClass) {
        return Arrays.stream(entityClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .peek(field -> field.setAccessible(true))
                .collect(Collectors.toList());
    }

    public Field getPrimaryKeyField(Class<?> entityClass) {
        return getAnnotatedFields(entityClass).stream()
                .filter(field -> field.getAnnotation(Column.class).primaryKey())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No primary key field found in class " + entityClass.getName()));
    }
}
