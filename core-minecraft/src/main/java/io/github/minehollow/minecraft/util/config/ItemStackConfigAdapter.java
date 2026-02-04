package io.github.minehollow.minecraft.util.config;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemStackConfigAdapter {

    public static ItemStack readFromSection(@NotNull ConfigurationSection section) {
        String typeName = section.getString("type", "AIR");
        Material material = Material.matchMaterial(typeName);
        if (material == null || material == Material.AIR) return new ItemStack(Material.AIR);

        int amount = section.getInt("amount", 1);
        ItemStack itemStack = new ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();

        if (meta == null) return itemStack;

        // --- Básico ---
        if (section.contains("name")) meta.setDisplayName(section.getString("name"));
        if (section.contains("lore")) meta.setLore(section.getStringList("lore"));
        if (section.contains("custom-model-data")) meta.setCustomModelData(section.getInt("custom-model-data"));

        // --- Encantamentos ---
        ConfigurationSection enchantSection = section.getConfigurationSection("enchants");
        if (enchantSection != null) {
            for (String key : enchantSection.getKeys(false)) {
                Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase()));
                if (enchant != null) meta.addEnchant(enchant, enchantSection.getInt(key), true);
            }
        }

        // --- Flags (HideEnchants, etc) ---
        List<String> flags = section.getStringList("flags");
        for (String flagName : flags) {
            try {
                meta.addItemFlags(ItemFlag.valueOf(flagName.toUpperCase()));
            } catch (Exception ignored) {
            }
        }

        // --- Metas Específicas ---

        // 1. Couro (Cores)
        if (meta instanceof LeatherArmorMeta leatherMeta && section.contains("color")) {
            leatherMeta.setColor(Color.fromRGB(section.getInt("color")));
        }

        // 2. Poções
        if (meta instanceof PotionMeta potionMeta && section.contains("potion-type")) {
            try {
                potionMeta.setBasePotionType(PotionType.valueOf(section.getString("potion-type", "AWKWARD").toUpperCase()));
            } catch (Exception ignored) {
            }
        }

        // 3. Livros Escritos
        if (meta instanceof BookMeta bookMeta) {
            if (section.contains("book-author")) bookMeta.setAuthor(section.getString("book-author"));
            if (section.contains("book-title")) bookMeta.setTitle(section.getString("book-title"));
            if (section.contains("pages")) bookMeta.setPages(section.getStringList("pages"));
        }

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public static void writeToSection(@NotNull ConfigurationSection section, @NotNull ItemStack itemStack) {
        if (itemStack.getType() == Material.AIR) return;

        section.set("type", itemStack.getType().name());
        section.set("amount", itemStack.getAmount());

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return;

        // --- Básico ---
        if (meta.hasDisplayName()) section.set("name", meta.getDisplayName());
        if (meta.hasLore()) section.set("lore", meta.getLore());
        if (meta.hasCustomModelData()) section.set("custom-model-data", meta.getCustomModelData());

        // --- Encantamentos ---
        if (meta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                section.set("enchants." + entry.getKey().getKey().getKey(), entry.getValue());
            }
        }

        // --- Flags ---
        if (!meta.getItemFlags().isEmpty()) {
            section.set("flags", meta.getItemFlags().stream().map(Enum::name).collect(Collectors.toList()));
        }

        // --- Metas Específicas ---

        if (meta instanceof LeatherArmorMeta leatherMeta) {
            section.set("color", leatherMeta.getColor().asRGB());
        }

        if (meta instanceof PotionMeta potionMeta) {
            section.set("potion-type", potionMeta.getBasePotionType().name());
        }

        if (meta instanceof BookMeta bookMeta) {
            if (bookMeta.hasAuthor()) section.set("book-author", bookMeta.getAuthor());
            if (bookMeta.hasTitle()) section.set("book-title", bookMeta.getTitle());
            if (bookMeta.hasPages()) section.set("pages", bookMeta.getPages());
        }
    }
}