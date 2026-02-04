package io.github.minehollow.mines.model.icon;

import io.github.minehollow.minecraft.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record MineMenuIcon(
  int slot,
  @NotNull Material type,
  @NotNull String displayName,
  @NotNull List<String> lore
) {

    public static MineMenuIcon createDefault() {
        return new MineMenuIcon(
          0,
          Material.STONE,
          "Icone não configurada",
          List.of("Por favor, configure esta icone.")
        );
    }

    public static MineMenuIcon readFromSection(@NotNull ConfigurationSection section) {
        int slot = section.getInt("slot", 0);
        Material type = Material.STONE;
        try {
            type = Material.valueOf(section.getString("type", "STONE").toUpperCase());
        } catch (IllegalArgumentException ignored) {
            throw new IllegalArgumentException("Material inválido para a icone do menu: " + section.getString("type"));
        }
        String displayName = section.getString("display-name", "Icone não configurada");
        List<String> lore = section.getStringList("lore");

        return new MineMenuIcon(slot, type, displayName, lore);
    }

    public Map<String, Object> toMap() {
        return Map.of(
          "slot", slot,
          "type", type.name(),
          "display-name", displayName,
          "lore", lore
        );
    }

    public ItemStack toItemStack() {
        return ItemBuilder.of(type)
          .name(displayName)
          .lore(lore)
          .flags(ItemFlag.values())
          .build();
    }
}
