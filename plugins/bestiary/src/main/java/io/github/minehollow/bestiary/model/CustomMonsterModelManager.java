package io.github.minehollow.bestiary.model;

import io.github.minehollow.bestiary.BestiaryPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CustomMonsterModelManager {

    private final BestiaryPlugin plugin;
    private final Map<String, CustomMonsterModel> modelsById = new HashMap<>();

    public CustomMonsterModelManager(@NotNull BestiaryPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadModels() {
        final var config = plugin.getConfig();
        final var section = config.getConfigurationSection("custom-monsters");
        if (section == null) {
            plugin.getLogger().warning("No custom monsters found in configuration.");
            return;
        }


        section.getKeys(false)
            .stream()
            .map(section::getConfigurationSection)
            .filter(Objects::nonNull)
            .map(CustomMonsterModel::readFromSection)
            .forEach(this::registerModel);
    }

    public @Nullable CustomMonsterModel getModelIfPresent(@NotNull String id) {
        return modelsById.get(id);
    }

    public void registerModel(@NotNull CustomMonsterModel model) {
        modelsById.put(model.getId(), model);
    }

    public void invalidateModel(@NotNull String id) {
        modelsById.remove(id);
    }

    public void deleteModel(@NotNull String id) {
        invalidateModel(id);

        final var config = plugin.getConfig();
        final var section = config.getConfigurationSection("custom-monsters");
        if (section != null) {
            section.set(id, null);
            plugin.saveConfig();
        }
    }

    public void saveModel(@NotNull CustomMonsterModel model) {
        final var config = plugin.getConfig();

        var section = config.getConfigurationSection("custom-monsters");
        if (section == null) {
            section = config.createSection("custom-monsters");
        }

        model.writeToSection(section);
        plugin.saveConfig();
    }

    public Collection<CustomMonsterModel> getAllModels() {
        return modelsById.values();
    }
}
