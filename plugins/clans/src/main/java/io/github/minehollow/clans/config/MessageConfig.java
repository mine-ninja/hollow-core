package io.github.minehollow.clans.config;

import io.github.minehollow.minecraft.util.message.StringUtils;
import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

/**
 * Loads and caches all player-facing messages from {@code messages.yml}.
 * Supports MiniMessage formatting and {@code {placeholder}} replacement.
 */
public class MessageConfig {

    @Getter
    private static MessageConfig instance;

    private final JavaPlugin plugin;
    private YamlConfiguration config;

    public MessageConfig(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        reload();
    }

    /**
     * (Re)loads messages.yml from disk, saving the default if missing.
     */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    // ─── Raw getters ──────────────────────────────────

    /**
     * Returns a single message string for the given key, with placeholders replaced.
     */
    public @NotNull String get(@NotNull String key, @NotNull Object... replacements) {
        String raw = config.getString(key);
        if (raw == null) return "§c[Missing message: " + key + "]";
        return applyPlaceholders(raw, replacements);
    }

    /**
     * Returns a multi-line message (string list) for the given key.
     */
    public @NotNull List<String> getList(@NotNull String key, @NotNull Object... replacements) {
        List<String> list = config.getStringList(key);
        if (list.isEmpty()) {
            String single = config.getString(key);
            if (single != null) return List.of(applyPlaceholders(single, replacements));
            return List.of("§c[Missing message: " + key + "]");
        }
        return list.stream()
            .map(line -> applyPlaceholders(line, replacements))
            .toList();
    }

    // ─── Send helpers ─────────────────────────────────

    /**
     * Sends a single-line message to a player using MiniMessage.
     */
    public void send(@NotNull Player player, @NotNull String key, @NotNull Object... replacements) {
        StringUtils.send(player, get(key, replacements));
    }

    /**
     * Sends a multi-line message to a player using MiniMessage.
     */
    public void sendList(@NotNull Player player, @NotNull String key, @NotNull Object... replacements) {
        List<String> lines = getList(key, replacements);
        StringUtils.send(player, lines.toArray(new String[0]));
    }

    /**
     * Sends a raw message string (already resolved) to a player.
     */
    public void sendRaw(@NotNull Player player, @NotNull String message) {
        StringUtils.send(player, message);
    }

    // ─── Placeholder engine ───────────────────────────

    /**
     * Replaces {key} placeholders in pairs: "tag", value, "name", value, ...
     */
    private @NotNull String applyPlaceholders(@NotNull String text, @NotNull Object[] replacements) {
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

