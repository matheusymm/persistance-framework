package com.persistence.utils;

import java.lang.reflect.Field;

import com.persistence.annotation.Column;

public class ColumnHelper {
    public String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            Column columnAnnotation = field.getAnnotation(Column.class);
            return columnAnnotation.name();
        }
        return field.getName().toLowerCase();
    }
}
