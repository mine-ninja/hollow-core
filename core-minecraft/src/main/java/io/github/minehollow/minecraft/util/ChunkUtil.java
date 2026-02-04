package io.github.minehollow.minecraft.util;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class ChunkUtil {

    public static long pack(@NotNull Location loc){
        int chunkX = loc.getBlockX() >> 4;
        int chunkZ = loc.getBlockZ() >> 4;
        return pack(chunkX, chunkZ);
    }

    public static long pack(int x, int z) {
        return ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);
    }

    public static int unpackX(long packed) {
        return (int) (packed & 0xFFFFFFFFL);
    }

    public static int unpackZ(long packed) {
        return (int) (packed >>> 32);
    }
}