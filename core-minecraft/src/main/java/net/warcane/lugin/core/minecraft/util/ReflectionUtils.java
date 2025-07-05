package net.warcane.lugin.core.minecraft.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ReflectionUtils {

    public static <T> T modifyClass(Class<?> clazz, T objectToModify, Modifier<?>... modifiers) {
        for (Modifier<?> modifier : modifiers) {
            try {
                Field fieldToModify = clazz.getDeclaredField(modifier.fieldName);
                fieldToModify.setAccessible(true);
                fieldToModify.set(objectToModify, modifier.fieldValue);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return objectToModify;
    }

    public static <T> T modifyClass(T objectToModify, Modifier<?>... modifiers) {
        return modifyClass(objectToModify.getClass(), objectToModify, modifiers);
    }

    public static Object getField(Class<?> clazz, Object objectToModify, String fieldName) {
        try {
            Field fieldToGet = clazz.getDeclaredField(fieldName);
            fieldToGet.setAccessible(true);

            return fieldToGet.get(objectToModify);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static Object createClass(Class<?> clazz, Class<?>[] classes, Object[] objects) {
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor(classes);
            constructor.setAccessible(true);

            return constructor.newInstance(objects);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException
                 | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Modifier<T> {
        String fieldName;
        T fieldValue;

        public Modifier(String fieldName, T fieldValue) {
            this.fieldName = fieldName;
            this.fieldValue = fieldValue;
        }
    }

}
