package io.github.minehollow.hologram.line;

import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a single line in a hologram, backed by a display entity.
 */
public interface HologramLine {

    /**
     * Returns the type of this line (TEXT, ITEM, BLOCK).
     */
    default @NotNull HologramLineType getType() {
        return HologramLineType.TEXT;
    }

    /**
     * Spawns the underlying entity at the given location.
     */
    void spawn(@NotNull Location location);

    /**
     * Despawns and removes the underlying entity.
     */
    void despawn();

    /**
     * Teleports the underlying entity to a new location.
     */
    void teleport(@NotNull Location location);

    /**
     * Adds a viewer to this line.
     */
    void addViewer(@NotNull UUID uuid);

    /**
     * Removes a viewer from this line.
     */
    void removeViewer(@NotNull UUID uuid);

    /**
     * Returns the underlying EntityLib wrapper entity, or null if not spawned.
     */
    WrapperEntity getEntity();

    /**
     * Returns the vertical height consumed by this line (used for layout spacing).
     */
    double getHeight();

    /**
     * Serializes this line's data for YAML persistence.
     */
    @NotNull Map<String, Object> serialize();
}
