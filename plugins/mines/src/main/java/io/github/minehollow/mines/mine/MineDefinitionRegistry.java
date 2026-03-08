package io.github.minehollow.mines.mine;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Slf4j
public final class MineDefinitionRegistry {

    private final Map<String, MineDefinition> definitions = new LinkedHashMap<>();

    private String defaultMineId;

    public void reload(@NotNull FileConfiguration config) {
        this.defaultMineId = config.getString("default-mine-id", "default");

        final Map<String, MineDefinition> loaded = new LinkedHashMap<>();
        final ConfigurationSection minesSection = config.getConfigurationSection("mines");
        if (minesSection == null) {
            this.definitions.clear();
            log.warn("No 'mines' section found in config.yml.");
            return;
        }

        int failed = 0;
        for (String key : minesSection.getKeys(false)) {
            final ConfigurationSection mineSection = minesSection.getConfigurationSection(key);
            if (mineSection == null) {
                failed++;
                log.warn("Skipping mine '{}': section is empty or invalid.", key);
                continue;
            }

            try {
                final MineDefinition definition = MineDefinition.readFromSection(mineSection);
                loaded.put(definition.getId().toLowerCase(), definition);
            } catch (Exception exception) {
                failed++;
                log.warn("Skipping mine '{}': {}", key, exception.getMessage());
            }
        }

        this.definitions.clear();
        this.definitions.putAll(loaded);

        if (failed > 0) {
            log.warn("Loaded {} mine definitions ({} failed).", loaded.size(), failed);
        }
    }

    public @Nullable MineDefinition findById(@NotNull String id) {
        return this.definitions.get(id.toLowerCase());
    }

    public @NotNull Collection<MineDefinition> getAll() {
        return Collections.unmodifiableCollection(this.definitions.values());
    }

    public void upsert(@NotNull MineDefinition definition) {
        this.definitions.put(definition.getId().toLowerCase(), definition);
    }

    public void setDefaultMineId(@NotNull String defaultMineId) {
        this.defaultMineId = defaultMineId.toLowerCase();
    }

    public @Nullable MineDefinition remove(@NotNull String id) {
        return this.definitions.remove(id.toLowerCase());
    }
}
