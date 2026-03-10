package io.github.minehollow.hologram;

import io.github.minehollow.hologram.line.HologramLine;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Represents a multi-line hologram composed of display entities.
 * Lines are positioned vertically from a base location, stacking upwards.
 * <p>
 * This class manages the lifecycle of its lines and viewer visibility.
 */
public class Hologram {

    private final String id;
    private Location location;
    private double lineSpacing;
    private boolean persistent;

    private final List<HologramLine> lines = new CopyOnWriteArrayList<>();
    private final Set<UUID> viewers = new CopyOnWriteArraySet<>();
    private boolean spawned = false;

    public Hologram(@NotNull String id, @NotNull Location location) {
        this(id, location, 0.3, true);
    }

    public Hologram(@NotNull String id, @NotNull Location location, double lineSpacing, boolean persistent) {
        this.id = id;
        this.location = location;
        this.lineSpacing = lineSpacing;
        this.persistent = persistent;
    }

    // ── ID & Location ────────────────────────────────────────

    public @NotNull String getId() {
        return id;
    }

    public @NotNull Location getLocation() {
        return location;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    public double getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(double lineSpacing) {
        this.lineSpacing = lineSpacing;
        if (spawned) {
            rebuildLines();
        }
    }

    // ── Line management ──────────────────────────────────────

    /**
     * Returns an unmodifiable view of the lines in this hologram.
     */
    public @NotNull List<HologramLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /**
     * Gets a line by index, or null if out of range.
     */
    public @Nullable HologramLine getLine(int index) {
        if (index < 0 || index >= lines.size()) return null;
        return lines.get(index);
    }

    /**
     * Adds a new line to the bottom of the hologram and rebuilds layout.
     */
    public void addLine(@NotNull HologramLine line) {
        lines.add(line);
        if (spawned) {
            rebuildLines();
        }
    }

    /**
     * Inserts a line at the specified index and rebuilds layout.
     */
    public void insertLine(int index, @NotNull HologramLine line) {
        if (index < 0) index = 0;
        if (index > lines.size()) index = lines.size();
        lines.add(index, line);
        if (spawned) {
            rebuildLines();
        }
    }

    /**
     * Replaces a line at the specified index and rebuilds layout.
     */
    public void setLine(int index, @NotNull HologramLine line) {
        if (index < 0 || index >= lines.size()) return;
        HologramLine old = lines.set(index, line);
        if (spawned) {
            old.despawn();
            rebuildLines();
        }
    }

    /**
     * Removes a line at the specified index and rebuilds layout.
     */
    public void removeLine(int index) {
        if (index < 0 || index >= lines.size()) return;
        HologramLine removed = lines.remove(index);
        if (spawned) {
            removed.despawn();
            rebuildLines();
        }
    }

    // ── Spawn / Despawn ──────────────────────────────────────

    /**
     * Spawns all lines at the appropriate positions.
     */
    public void spawn() {
        if (spawned) return;
        spawned = true;
        spawnAllLines();
    }

    /**
     * Despawns all lines and clears viewers.
     */
    public void despawn() {
        if (!spawned) return;
        spawned = false;
        for (HologramLine line : lines) {
            line.despawn();
        }
        viewers.clear();
    }

    public boolean isSpawned() {
        return spawned;
    }

    /**
     * Teleports the hologram to a new location and repositions all lines.
     */
    public void teleport(@NotNull Location location) {
        this.location = location;
        if (spawned) {
            rebuildLines();
        }
    }

    // ── Viewer management ────────────────────────────────────

    public void addViewer(@NotNull Player player) {
        if (!spawned) return;
        UUID uuid = player.getUniqueId();
        if (!viewers.add(uuid)) return;

        for (HologramLine line : lines) {
            line.addViewer(uuid);
        }
    }

    public void removeViewer(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        if (!viewers.remove(uuid)) return;

        for (HologramLine line : lines) {
            line.removeViewer(uuid);
        }
    }

    public boolean isViewer(@NotNull UUID uuid) {
        return viewers.contains(uuid);
    }

    public @NotNull Set<UUID> getViewers() {
        return Collections.unmodifiableSet(viewers);
    }

    // ── Internal layout ──────────────────────────────────────

    private void spawnAllLines() {
        if (lines.isEmpty()) return;

        for (int i = 0; i < lines.size(); i++) {
            Location lineLoc = calculateLineLocation(i);
            lines.get(i).spawn(lineLoc);
        }
    }

    /**
     * Despawns all existing lines and re-spawns them at their correct positions.
     * Re-adds all current viewers.
     */
    private void rebuildLines() {
        Set<UUID> currentViewers = new HashSet<>(viewers);

        for (HologramLine line : lines) {
            line.despawn();
        }

        spawnAllLines();

        for (UUID uuid : currentViewers) {
            for (HologramLine line : lines) {
                line.addViewer(uuid);
            }
        }
    }

    /**
     * Calculates the location for a line at the given index.
     * Lines stack upward: index 0 is the bottom line, last index is the top.
     */
    private @NotNull Location calculateLineLocation(int index) {
        double yOffset = 0;
        for (int i = 0; i < index; i++) {
            yOffset += lines.get(i).getHeight() + lineSpacing;
        }
        return location.clone().add(0, yOffset, 0);
    }
}
