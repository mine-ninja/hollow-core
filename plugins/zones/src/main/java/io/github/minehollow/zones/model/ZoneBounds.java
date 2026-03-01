package io.github.minehollow.zones.model;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Axis-aligned bounding box for a zone.
 */
public record ZoneBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX
            && y >= minY && y <= maxY
            && z >= minZ && z <= maxZ;
    }

    public boolean contains(@NotNull Location loc) {
        return contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    /**
     * Checks if this bounds intersects a 16×16 chunk column at the given chunk coords.
     */
    public boolean intersectsChunk(int chunkX, int chunkZ) {
        int cx0 = chunkX << 4;
        int cz0 = chunkZ << 4;
        int cx1 = cx0 + 15;
        int cz1 = cz0 + 15;
        return maxX >= cx0 && minX <= cx1 && maxZ >= cz0 && minZ <= cz1;
    }
}

