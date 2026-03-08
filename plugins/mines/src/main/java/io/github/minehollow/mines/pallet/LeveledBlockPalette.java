package io.github.minehollow.mines.pallet;

import io.github.minehollow.mines.util.CachedBlockData;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Slf4j
public record LeveledBlockPalette(
    @NotNull Map<BlockData, Double> blockChances
) {

    public static LeveledBlockPalette readFromSection(@Nullable ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Section cannot be null");
        }

        // Formato novo: block-palette.blocks.<MATERIAL> = chance
        // Formato legado: block-palettes.<MATERIAL> = chance
        ConfigurationSection blockChanceSection = section.getConfigurationSection("blocks");
        if (blockChanceSection == null) {
            blockChanceSection = section;
        }

        final Map<BlockData, Double> blockChances = new LinkedHashMap<>();
        for (String key : blockChanceSection.getKeys(false)) {
            final Material material = Material.matchMaterial(key);
            if (material == null) {
                log.warn("Invalid material '{}' in section '{}'", key, section.getCurrentPath());
                continue;
            }

            final BlockData blockData = CachedBlockData.get(material);
            final double chance = blockChanceSection.getDouble(key, -1);
            if (chance <= 0) {
                log.warn("Invalid chance '{}' for block '{}' in section '{}'", chance, key, section.getCurrentPath());
                continue;
            }

            blockChances.put(blockData, chance);
        }

        return new LeveledBlockPalette(blockChances);
    }

    public void writeToSection(@NotNull ConfigurationSection section, @NotNull String key) {
        final var paletteSection = section.createSection(key);
        final var blockChanceSection = paletteSection.createSection("blocks");
        for (Map.Entry<BlockData, Double> entry : blockChances.entrySet()) {
            blockChanceSection.set(entry.getKey().getMaterial().toString(), entry.getValue());
        }
    }

    public void addBlock(@NotNull BlockData blockData, double chance) {
        blockChances.put(blockData, chance);
    }

    public @NotNull BlockData pickBlock(double normalizedNoise) {
        if (blockChances.isEmpty()) {
            return CachedBlockData.get(Material.STONE);
        }

        final double clamped = Math.max(0.0D, Math.min(1.0D, normalizedNoise));
        double totalWeight = 0.0D;
        for (double value : blockChances.values()) {
            totalWeight += value;
        }

        if (totalWeight <= 0.0D) {
            return blockChances.keySet().iterator().next();
        }

        final double target = clamped * totalWeight;
        double current = 0.0D;
        BlockData fallback = null;

        for (Map.Entry<BlockData, Double> entry : blockChances.entrySet()) {
            fallback = entry.getKey();
            current += entry.getValue();
            if (current >= target) {
                return entry.getKey();
            }
        }

        return fallback != null ? fallback : CachedBlockData.get(Material.STONE);
    }
}
