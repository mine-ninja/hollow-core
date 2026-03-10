package io.github.minehollow.mines.pickaxe;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PickaxeManager {

    private static final NamespacedKey PICKAXE_KEY = new NamespacedKey("mines", "pickaxe");
    private static final NamespacedKey PICKAXE_LEVEL_KEY = new NamespacedKey("mines", "pickaxe_level");


    private static final Cache<String, NamespacedKey> CUSTOM_ENCHANTMENT_KEY_CACHE = Caffeine.newBuilder()
        .build(key -> new NamespacedKey("mines", "custom_enchantment_" + key));

    private static final ItemStack DEFAULT_PICKAXE = ItemBuilder.of(Material.WOODEN_PICKAXE)
        .name("<light_purple>Picareta")
        .enchant(Enchantment.EFFICIENCY, 100)
        .nbt(pdc -> pdc.set(PICKAXE_KEY, PersistentDataType.BOOLEAN, true))
        .lore(
            ""
        )
        .build();


    public static @Nullable NamespacedKey getCustomEnchantmentKey(String customEnchantmentId) {
        return CUSTOM_ENCHANTMENT_KEY_CACHE.get(customEnchantmentId, key -> new NamespacedKey("mines", "custom_enchantment_" + key));
    }

    public static ItemStack createPickaxe(int level) {
        return ItemBuilder.of(DEFAULT_PICKAXE)
            .nbt(pdc -> pdc.set(PICKAXE_LEVEL_KEY, PersistentDataType.INTEGER, level))
            .flags(ItemFlag.values())
            .meta(meta ->{
                meta.setUnbreakable(true);
                meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            })
            .lore(
                "",
                "<gray>Nível: <white>" + level,
                "",
                "<gray>Encantamentos: ",
                " <gray>Eficiência: <white>∞",
                ""
            )
            .build();
    }


    public static long getCustomEnchantmentLevel(@NotNull ItemStack item, @NotNull String customEnchantmentId) {
        if (!item.hasItemMeta()) {
            return 0;
        }

        final var meta = item.getItemMeta();
        final var key = getCustomEnchantmentKey(customEnchantmentId);
        if (key == null) {
            return 0;
        }
        if (!meta.getPersistentDataContainer().has(key, PersistentDataType.LONG)) {
            return 0;
        }
        final var levelObj = meta.getPersistentDataContainer().get(key, PersistentDataType.LONG);
        return levelObj != null ? levelObj : 0;
    }

    public static boolean isPickaxe(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }

        final var meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(PICKAXE_KEY, PersistentDataType.BOOLEAN);
    }
}
