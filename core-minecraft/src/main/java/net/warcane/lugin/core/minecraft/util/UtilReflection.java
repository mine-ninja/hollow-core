package net.warcane.lugin.core.minecraft.util;

import lombok.experimental.UtilityClass;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class UtilReflection {

    private static final Map<String, Map<String, Field>> FIELDS_CACHE = new ConcurrentHashMap<>();

    public static void removeFinal(Field field) throws Exception {
        int modifiers = field.getModifiers();
        Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
        getDeclaredFields0.setAccessible(true);
        Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
        Field modifiersField = null;
        for (Field each : fields) {
            if ("modifiers".equals(each.getName())) {
                modifiersField = each;
                break;
            }
        }
        Objects.requireNonNull(modifiersField);
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, modifiers & ~Modifier.FINAL);
    }

    public static boolean setField(Object instance, String field, Object value) {
        return setField(instance.getClass(), instance, field, value);
    }

    public static boolean setField(Class<?> clazz, Object instance, String field, Object value) {
        try {
            Field declaredField = clazz.getDeclaredField(field);
            setFieldAccessible(declaredField);
            declaredField.set(instance, value);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void setFieldValue(Object instance, String fieldName, Object value) {
        try {
            Class<?> clazz = instance.getClass();
            Map<String, Field> classCache = FIELDS_CACHE.computeIfAbsent(clazz.getName(), k -> new LinkedHashMap<>());
            Field f = classCache.computeIfAbsent(fieldName, fn -> {
                try {
                    Field field = clazz.getDeclaredField(fn);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            });
            f.set(instance, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    public static <T> T getFieldValue(Object instance, String fieldName) {
        try {
            Class<?> clazz = instance.getClass();
            Map<String, Field> classCache = FIELDS_CACHE.computeIfAbsent(clazz.getName(), k -> new LinkedHashMap<>());
            Field f = classCache.computeIfAbsent(fieldName, fn -> {
                try {
                    Field field = clazz.getDeclaredField(fn);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException e) {
                    throw new RuntimeException(e);
                }
            });
            return (T) f.get(instance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field getFirstFieldOf(Class<?> clazz, Class<?> objclass) throws Exception {
        Field f = null;
        for (Field value : clazz.getDeclaredFields()) {
            if (value.getType().equals(objclass)) {
                f = value;
                break;
            }
        }

        if (f == null) {
            for (Field field : clazz.getFields()) {
                if (field.getType().equals(objclass)) {
                    f = field;
                    break;
                }
            }
        }

        if (f != null)
            setFieldAccessible(f);
        return f;
    }

    public static void setFieldAccessible(Field f) {
        f.setAccessible(true);
    }

    public static void setFirstFieldOf(Class<?> clazz, Object instance, Class<?> objclass, Object toset) throws Exception {
        Field f = getFirstFieldOf(clazz, objclass);
        if (f != null) {
            f.set(instance, toset);
        } else {
            throw new Exception("setFirstFieldOf failed with field " + objclass);
        }
    }

}
