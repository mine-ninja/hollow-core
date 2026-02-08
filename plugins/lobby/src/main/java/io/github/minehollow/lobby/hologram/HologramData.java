package io.github.minehollow.lobby.hologram;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record HologramData(
        @NotNull String id,
        @NotNull Location location,
        @NotNull List<String> lines
) {

    public @NotNull String getCombinedLines() {
        return String.join("\n", lines);
    }
}