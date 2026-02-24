package io.github.minehollow.minecraft.misc.chunk;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

public final class ChunkPlayerCountCache {

    private static final Long2IntOpenHashMap HEATMAP = new Long2IntOpenHashMap();
    private static final int RADIUS = 4; // Ajuste o raio de ativação aqui

    static {
        HEATMAP.defaultReturnValue(0);
    }

    public static boolean isChunkActive(final long key) {
        return HEATMAP.get(key) > 0;
    }

    public static int getPlayerCount(final long key) {
        return HEATMAP.get(key);
    }

    public static void updatePresence(final int chunkX, final int chunkZ, final int delta) {
        for (int x = chunkX - RADIUS; x <= chunkX + RADIUS; x++) {
            for (int z = chunkZ - RADIUS; z <= chunkZ + RADIUS; z++) {
                final long key = ((long) x & 0xFFFFFFFFL) | (((long) z & 0xFFFFFFFFL) << 32);

                final int newValue = HEATMAP.get(key) + delta;
                if (newValue <= 0) {
                    HEATMAP.remove(key);
                } else {
                    HEATMAP.put(key, newValue);
                }
            }
        }
    }
}