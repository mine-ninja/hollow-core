package io.github.minehollow.minecraft.util.nbt;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NbtUtil {

    public static <T> @Nullable T supplyFromItemPersistentData(
        @NotNull ItemStack itemStack,
        @NotNull Function<PersistentDataContainer , T> supplier
    ) {
        if (itemStack.getItemMeta() == null) {
            return null;
        }

        final var container = itemStack.getItemMeta().getPersistentDataContainer();
        return supplier.get();
    }

    public static void useItemPersistentData(
        @NotNull ItemStack itemStack,
        @NotNull Consumer<PersistentDataContainer> consumer
    ) {
        if (itemStack.getItemMeta() == null) {
            return;
        }

        itemStack.editMeta(meta -> consumer.accept(meta.getPersistentDataContainer()));
    }

    public static <P, C> C get(
        @NotNull ItemStack itemStack,
        @NotNull String prefix,
        @NotNull String key,
        @NotNull PersistentDataType<P, C> type
    ) {
        if (itemStack.getItemMeta() == null) {
            return null;
        }

        return get(itemStack.getItemMeta().getPersistentDataContainer(), prefix, key, type);
    }

    public static <P, C> void set(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key,
        @NotNull PersistentDataType<P, C> type,
        @NotNull C value
    ) {
        container.set(new NamespacedKey(prefix, key), type, value);
    }

    public static <P, C> @Nullable C get(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key,
        @NotNull PersistentDataType<P, C> type
    ) {
        final var namespacedKey = new NamespacedKey(prefix, key);
        if (!container.has(namespacedKey, type)) {
            return null;
        }
        return container.get(namespacedKey, type);
    }


    public static void setString(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key,
        @NotNull String value
    ) {
        container.set(new NamespacedKey(prefix, key), PersistentDataType.STRING, value);
    }

    public static @Nullable String getString(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key
    ) {
        return container.get(new NamespacedKey(prefix, key), PersistentDataType.STRING);
    }

    public static void setInt(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key,
        int value
    ) {
        container.set(new NamespacedKey(prefix, key), PersistentDataType.INTEGER, value);
    }

    public static int getInt(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key
    ) {
        Integer val = container.get(new NamespacedKey(prefix, key), PersistentDataType.INTEGER);
        return val != null ? val : 0;
    }

    public static void setBoolean(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key,
        boolean value
    ) {
        container.set(new NamespacedKey(prefix, key), PersistentDataType.BYTE, (byte) (value ? 1 : 0));
    }

    public static boolean getBoolean(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key
    ) {
        Byte b = container.get(new NamespacedKey(prefix, key), PersistentDataType.BYTE);
        return b != null && b == 1;
    }


    public static boolean has(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key
    ) {
        return container.has(new NamespacedKey(prefix, key));
    }

    public static void remove(
        @NotNull PersistentDataContainer container,
        @NotNull String prefix,
        @NotNull String key
    ) {
        container.remove(new NamespacedKey(prefix, key));
    }
}
