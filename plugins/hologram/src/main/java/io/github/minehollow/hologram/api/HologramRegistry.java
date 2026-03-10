package io.github.minehollow.hologram.api;

import io.github.minehollow.hologram.Hologram;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Central registry for all holograms. Accessible via {@code HologramPlugin.getRegistry()}.
 * <p>
 * Other plugins can use this to create and manage holograms programmatically:
 * <pre>{@code
 *   HologramRegistry registry = HologramPlugin.getInstance().getRegistry();
 *   Hologram holo = registry.create("my-hologram", location);
 *   holo.addLine(new TextDisplayHologramLine("<gold>Hello World!"));
 * }</pre>
 */
public interface HologramRegistry {

    /**
     * Creates a new hologram at the given location. The hologram is
     * immediately spawned and marked as persistent (will be saved to disk).
     *
     * @param id       unique identifier
     * @param location base location
     * @return the created hologram
     * @throws IllegalArgumentException if a hologram with this id already exists
     */
    @NotNull Hologram create(@NotNull String id, @NotNull Location location);

    /**
     * Creates a non-persistent (temporary) hologram. It will NOT be saved to disk.
     *
     * @param id       unique identifier
     * @param location base location
     * @return the created hologram
     */
    @NotNull Hologram createTemporary(@NotNull String id, @NotNull Location location);

    /**
     * Gets a hologram by its ID, or null if not found.
     */
    @Nullable Hologram get(@NotNull String id);

    /**
     * Returns all registered holograms.
     */
    @NotNull Collection<Hologram> getAll();

    /**
     * Removes and despawns a hologram.
     *
     * @return true if the hologram existed and was removed
     */
    boolean remove(@NotNull String id);
}

