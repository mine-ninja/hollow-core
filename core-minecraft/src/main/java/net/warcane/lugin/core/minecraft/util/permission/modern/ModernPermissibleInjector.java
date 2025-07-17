package net.warcane.lugin.core.minecraft.util.permission.modern;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

import java.lang.reflect.Field;

public final class ModernPermissibleInjector {

    private static final Field HUMAN_ENTITY_PERMISSIBLE_FIELD;

    static {
        try {
            Field humanEntityPermissibleField;
            humanEntityPermissibleField = CraftBukkitImplementation.obcClass("entity.CraftHumanEntity").getDeclaredField("perm");
            humanEntityPermissibleField.setAccessible(true);

            HUMAN_ENTITY_PERMISSIBLE_FIELD = humanEntityPermissibleField;
        } catch (ClassNotFoundException | NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void inject(Player player, Permissible newPermissible) {
        try {
            HUMAN_ENTITY_PERMISSIBLE_FIELD.set(player, newPermissible);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to inject permissible into player", e);
        }
    }
}