package net.warcane.lugin.core.minecraft.util;

import net.minecraft.server.v1_8_R3.Entity;

import java.lang.reflect.Field;

public class EntityIdUtil {

    private static Field entityCountField;

    static {
        try {
            entityCountField = Entity.class.getDeclaredField("entityCount");
            entityCountField.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized int nextEntityId() {
        try {
            int currentId = entityCountField.getInt(null);
            entityCountField.set(null, currentId + 1);
            return currentId;
        } catch (Exception e) {
            return -1;
        }
    }
}
