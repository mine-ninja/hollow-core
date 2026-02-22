package io.github.minehollow.bestiary.model;

import io.github.minehollow.minecraft.util.range.DoubleRange;
import io.github.minehollow.minecraft.util.range.IntRange;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class CustomMonsterModel {

    private String id;
    private String displayName;
    private EntityType entityType;
    private IntRange levelRange;
    private DoubleRange healthPerLevelRange;
    private DoubleRange damagePerLevelRange;
    private DoubleRange defensePerLevelRange;
    private Map<EquipmentSlot, ItemStack> equipment;
    private Map<ItemStack, Double> possibleDrops;


    public static CustomMonsterModel readFromSection(@NotNull ConfigurationSection section) {
        final String id = section.getString("id", section.getName());
        final String displayName = section.getString("display-name", id);
        final EntityType entityType = EntityType.valueOf(section.getString("entity-type", "ZOMBIE").toUpperCase());
        final IntRange levelRange = IntRange.parseString(section.getString("level-range", "1-10"));
        final DoubleRange healthPerLevel = DoubleRange.parseString(section.getString("health-per-level", "10-20"));
        final DoubleRange damagePerLevel = DoubleRange.parseString(section.getString("damage-per-level", "1-5"));
        final DoubleRange defensePerLevel = DoubleRange.parseString(section.getString("defense-per-level", "0-3"));

        final Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
        final ConfigurationSection equipmentSection = section.getConfigurationSection("equipment");

        if (equipmentSection != null) {
            for (String key : equipmentSection.getKeys(false)) {
                final ItemStack item = equipmentSection.getItemStack(key);
                if (item == null) continue;
                try {
                    equipment.put(EquipmentSlot.valueOf(key.toUpperCase()), item);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        final Map<ItemStack, Double> possibleDrops = new HashMap<>();
        final ConfigurationSection dropsSection = section.getConfigurationSection("possible-drops");

        if (dropsSection != null) {
            for (String key : dropsSection.getKeys(false)) {
                final ConfigurationSection dropEntry = dropsSection.getConfigurationSection(key);
                if (dropEntry == null) continue;

                final ItemStack item = dropEntry.getItemStack("item");
                final double chance = dropEntry.getDouble("chance", 1.0);

                if (item != null) {
                    possibleDrops.put(item, chance);
                }
            }
        }

        return new CustomMonsterModel(
            id, displayName, entityType,
            levelRange, healthPerLevel, damagePerLevel, defensePerLevel,
            equipment, possibleDrops
        );
    }


    public void writeToSection(@NotNull ConfigurationSection rootSection) {
        final var section = rootSection.createSection(id);
        section.set("display-name", displayName);
        section.set("entity-type", entityType.name());
        section.set("level-range", levelRange.toString());
        section.set("health-per-level", healthPerLevelRange.toString());
        section.set("damage-per-level", damagePerLevelRange.toString());
        section.set("defense-per-level", defensePerLevelRange.toString());

        final ConfigurationSection equipSection = section.createSection("equipment");
        for (Map.Entry<EquipmentSlot, ItemStack> entry : equipment.entrySet()) {
            equipSection.set(entry.getKey().name().toLowerCase(), entry.getValue());
        }

        final ConfigurationSection dropsSection = section.createSection("possible-drops");
        int count = 0;
        for (Map.Entry<ItemStack, Double> entry : possibleDrops.entrySet()) {
            final ConfigurationSection dropEntry = dropsSection.createSection("drop_" + count++);
            dropEntry.set("item", entry.getKey());
            dropEntry.set("chance", entry.getValue());
        }
    }
}
