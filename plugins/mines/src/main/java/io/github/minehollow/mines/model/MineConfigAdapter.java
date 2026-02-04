package io.github.minehollow.mines.model;

import io.github.minehollow.mines.model.block.MineBlockConfig;
import io.github.minehollow.mines.model.icon.MineMenuIcon;
import io.github.minehollow.mines.model.spawn.MineSpawnPosition;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public class MineConfigAdapter {

    public static void writeToSection(@NotNull Mine mine, @NotNull ConfigurationSection section) {
        final var mineRootSection = getOrCreateSection(section, mine.getId());

        final var spawnPositionSection = getOrCreateSection(mineRootSection, "spawn-position");
        mine.getSpawnPosition().toMap().forEach(spawnPositionSection::set);

        final var menuIconSection = getOrCreateSection(mineRootSection, "menu-icon");
        mine.getMenuIcon().toMap().forEach(menuIconSection::set);

        mineRootSection.set("chunks", mine.serializeChunks());
        mineRootSection.set("min-y", mine.getMinY());
        mineRootSection.set("max-y", mine.getMaxY());

        final var blocksSection = getOrCreateSection(mineRootSection, "blocks");
        mine.getBlockConfigs().forEach((material, cfg) -> {
            final var blockSection = getOrCreateSection(blocksSection, material.name());
            cfg.toMap().forEach(blockSection::set);
        });

        final var metadataSection = getOrCreateSection(mineRootSection, "metadata");
        mine.getMetadata().forEach(metadataSection::set);
    }

    public static Mine readFromSection(@NotNull ConfigurationSection section) {
        final String id = section.getName();
        final var spawnPosition = MineSpawnPosition.readFromSection(getSectionOrThrow(section, "spawn-position"));
        final var menuIcon = MineMenuIcon.readFromSection(getOrCreateSection(section, "menu-icon"));

        LongSet chunks = new LongOpenHashSet(section.getStringList("chunks")
          .stream()
          .mapToLong(Long::parseLong)
          .toArray());

        final int minY = section.getInt("min-y");
        final int maxY = section.getInt("max-y");
        final Map<Material, MineBlockConfig> blockConfigs = MineBlockConfig.readAllFromSection(
          getSectionOrThrow(section, "blocks")
        );

        Map<String, Object> metadata = Collections.emptyMap();
        final var metadataSection = section.getConfigurationSection("metadata");
        if (metadataSection != null) {
            metadata = metadataSection.getValues(false);
        }

        return new Mine(id, spawnPosition, menuIcon, chunks, minY, maxY, blockConfigs, metadata);
    }


    private static ConfigurationSection getSectionOrThrow(
      @NotNull ConfigurationSection root,
      @NotNull String path
    ) {
        final ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            throw new IllegalStateException("Missing required configuration section: " + path);
        }
        return section;
    }

    private static ConfigurationSection getOrCreateSection(
      @NotNull ConfigurationSection root,
      @NotNull String path
    ) {
        ConfigurationSection section = root.getConfigurationSection(path);
        if (section == null) {
            section = root.createSection(path);
        }
        return section;
    }
}
