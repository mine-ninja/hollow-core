package io.github.minehollow.mines.mine;

import io.github.minehollow.mines.instance.MineInstance;
import io.github.minehollow.mines.pallet.LeveledBlockPalette;
import io.github.minehollow.mines.util.CachedBlockData;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;

public final class VirtualMineBlockResolver {

    public @NotNull BlockData resolve(@NotNull MineInstance instance, int x, int y, int z) {
        if (instance.isMined(x, y, z)) {
            return CachedBlockData.get(Material.AIR);
        }

        final LeveledBlockPalette palette = instance.getDefinition().getBlockPalette();
        if (palette == null) {
            return CachedBlockData.get(Material.STONE);
        }

        final long hash = mix(instance.getSeed(), instance.currentEpoch(), x, y, z);
        final double noise = toUnit(hash);
        return palette.pickBlock(noise);
    }

    private static long mix(long seed, long epoch, int x, int y, int z) {
        long value = seed ^ 0x9E3779B97F4A7C15L;
        value ^= epoch * 0x9E3779B97F4A7C15L;
        value ^= (long) x * 0xC2B2AE3D27D4EB4FL;
        value ^= (long) y * 0x165667B19E3779F9L;
        value ^= (long) z * 0x85EBCA77C2B2AE63L;

        value ^= (value >>> 33);
        value *= 0xFF51AFD7ED558CCDL;
        value ^= (value >>> 33);
        value *= 0xC4CEB9FE1A85EC53L;
        value ^= (value >>> 33);
        return value;
    }

    private static double toUnit(long value) {
        return (value & Long.MAX_VALUE) / (double) Long.MAX_VALUE;
    }
}

