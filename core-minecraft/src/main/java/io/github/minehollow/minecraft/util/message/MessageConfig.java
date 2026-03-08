package io.github.minehollow.minecraft.util.message;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class MessageConfig {
    private static final String FILE_NAME = "messages.yml";
    private final Plugin plugin;
    private YamlConfiguration config;
    private final Map<String, Map<String, String>> sections = new HashMap<>();

    public MessageConfig(@NotNull Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            try {
                if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
                try (InputStream input = plugin.getResource(FILE_NAME)) {
                    if (input != null) Files.copy(input, file.toPath());
                    else file.createNewFile();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to create messages.yml", e);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        loadSections();
    }

    private void loadSections() {
        sections.clear();
        for (String section : config.getKeys(false)) {
            var sec = config.getConfigurationSection(section);
            if (sec == null) continue;
            Map<String, String> map = new HashMap<>();
            for (String key : sec.getKeys(false)) {
                map.put(key, sec.getString(key));
            }
            sections.put(section, map);
        }
    }

    public String getRaw(@NotNull String section, @NotNull String key, @NotNull Object... replacements) {
        Map<String, String> map = sections.get(section);
        String raw = map != null ? map.get(key) : null;
        if (raw == null) return "<red>[Missing message: " + section + "." + key + "]";
        return applyPlaceholders(raw, replacements);
    }

    public Component get(@NotNull String section, @NotNull String key, @NotNull Object... replacements) {
        return StringUtils.formatString(getRaw(section, key, replacements));
    }

    public void send(@NotNull Player player, @NotNull String section, @NotNull String key, @NotNull Object... replacements) {
        player.sendMessage(get(section, key, replacements));
    }

    private String applyPlaceholders(@NotNull String text, @NotNull Object[] replacements) {
        if (replacements.length < 2) return text;
        String result = text;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            String placeholder = "{" + replacements[i] + "}";
            String value = String.valueOf(replacements[i + 1]);
            result = result.replace(placeholder, value);
        }
        return result;
    }
}

