package com.manager.persistence.db;



import java.lang.reflect.Field;
import java.util.Date;

import com.manager.persistence.annotation.Column;

/**
 * 数据类型
 * 
 */
public enum TypeEnum {
    TINYINT,
    SMALLINT,
    INT,
    BIGINT,
    DOUBLE,
    CHAR,
    VARCHAR,
    TEXT,
    DATETIME,
    OBJ,
    ;

    public static TypeEnum parseFromTypeString(String type) {
        type = type.toUpperCase();
        for (TypeEnum tp : values()) {
            if (type.startsWith(tp.name())) {
                return tp;
            }
        }
        return null;
    }

    public static TypeEnum getTypeOfField(Field field) {
        Class<?> type = field.getType();
        if (type == Long.TYPE || type == Long.class)
            return BIGINT;
        if (type == Integer.TYPE || type == Integer.class)
            return INT;
        if (type == Short.TYPE || type == Short.class)
            return SMALLINT;
        if (type == Byte.TYPE || type == Byte.class)
            return TINYINT;
        if (type == Boolean.TYPE || type == Boolean.class)
            return TINYINT;
        if (type == Double.TYPE || type == Double.class || type == Float.TYPE || type == Float.class)
            return DOUBLE;
        if (type == String.class) {
        	Column column = field.getAnnotation(Column.class);
        	if (column.length() <= 255) {
        		if (column.immutable())
        			return CHAR;
        		else
        			return VARCHAR;
        	} else 
        		return TEXT;
        }
        if(type == Character.class){
            return CHAR;
        }
        if (type == Date.class)
            return DATETIME;
        if (type.isEnum())
            return TINYINT;
        else 
            return OBJ;
    }
}
