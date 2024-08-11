package org.embeddedt.vintagefix.util;

import java.lang.reflect.Field;

public class Reflector {
    public static Field findField(Class<?> clz, String fieldName) {
        try {
            Field f = clz.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f;
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException("Failed to lookup field " + fieldName + " on class " + clz.getName(), e);
        }
    }
}
