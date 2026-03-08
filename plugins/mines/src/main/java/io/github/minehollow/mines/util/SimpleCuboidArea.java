package io.github.minehollow.mines.util;

import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Data
public class SimpleCuboidArea {

    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    public static SimpleCuboidArea readFromSection(@Nullable ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Section cannot be null");
        }

        final var minX = section.getInt("min-x");
        final var minY = section.getInt("min-y");
        final var minZ = section.getInt("min-z");

        final var maxX = section.getInt("max-x");
        final var maxY = section.getInt("max-y");
        final var maxZ = section.getInt("max-z");

        return new SimpleCuboidArea(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public SimpleCuboidArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.minZ = Math.min(minZ, maxZ);

        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
        this.maxZ = Math.max(minZ, maxZ);
    }

    public void writeToSection(@NotNull ConfigurationSection root, @NotNull String name) {
        final var section = root.createSection(name);
        section.set("min-x", minX);
        section.set("min-y", minY);
        section.set("min-z", minZ);

        section.set("max-x", maxX);
        section.set("max-y", maxY);
        section.set("max-z", maxZ);
    }

    public int volume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }

    public int minChunkX() {
        return minX >> 4;
    }

    public int maxChunkX() {
        return maxX >> 4;
    }

    public int minChunkZ() {
        return minZ >> 4;
    }

    public int maxChunkZ() {
        return maxZ >> 4;
    }

    public boolean intersectsChunk(int chunkX, int chunkZ) {
        return chunkX >= minChunkX() && chunkX <= maxChunkX()
            && chunkZ >= minChunkZ() && chunkZ <= maxChunkZ();
    }

    public void forEachChunk(@NotNull ChunkAction action) {
        for (int chunkX = minChunkX(); chunkX <= maxChunkX(); chunkX++) {
            for (int chunkZ = minChunkZ(); chunkZ <= maxChunkZ(); chunkZ++) {
                action.accept(chunkX, chunkZ);
            }
        }
    }

    public void forEachBlocks(@NotNull SimpleBlockAction action) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    action.accept(x, y, z);
                }
            }
        }
    }

    public void forEachBlocksInChunk(int chunkX, int chunkZ, @NotNull SimpleBlockAction action) {
        if (!intersectsChunk(chunkX, chunkZ)) {
            return;
        }

        final int startX = Math.max(minX, chunkX << 4);
        final int endX = Math.min(maxX, (chunkX << 4) + 15);
        final int startZ = Math.max(minZ, chunkZ << 4);
        final int endZ = Math.min(maxZ, (chunkZ << 4) + 15);

        for (int x = startX; x <= endX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    action.accept(x, y, z);
                }
            }
        }
    }

    public String getSizeFormatted() {
        return (maxX - minX + 1) + "x"  + (maxZ - minZ + 1);
    }

    public int getWidthX() {
        return maxX - minX + 1;
    }

    public int getHeightY() {
        return maxY - minY + 1;
    }

    public int getDepthZ() {
        return maxZ - minZ + 1;
    }

    @FunctionalInterface
    public interface SimpleBlockAction {
        void accept(int x, int y, int z);
    }

    @FunctionalInterface
    public interface ChunkAction {
        void accept(int chunkX, int chunkZ);
    }
}