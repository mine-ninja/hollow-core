package io.github.minehollow.mines.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

public final class Messages {

    private final YamlConfiguration config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public Messages(@NotNull File file) {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public @NotNull Component mm(@NotNull String path) {
        final String raw = this.config.getString(path, "<red>Missing message: <white>" + path + "</white>");
        return this.miniMessage.deserialize(raw);
    }

    public @NotNull Component mm(@NotNull String path, @NotNull Map<String, String> placeholders) {
        String raw = this.config.getString(path, "<red>Missing message: <white>" + path + "</white>");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("<" + entry.getKey() + ">", entry.getValue());
        }

        return this.miniMessage.deserialize(raw);
    }

    public @NotNull List<Component> mmList(@NotNull String path) {
        final List<String> list = this.config.getStringList(path);
        final List<Component> out = new ArrayList<>(list.size());
        for (String line : list) {
            out.add(this.miniMessage.deserialize(line));
        }

        return out;
    }
}

