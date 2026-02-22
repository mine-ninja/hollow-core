package io.github.minehollow.minecraft.chunk;

import io.github.minehollow.event.MineHollowOptimizations;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChunkTickHandler {

    public static void register() {
        MineHollowOptimizations.registerPreTickHandler((world, chunkX, chunkZ) -> {
            if (isNearbyChunkSpawn(chunkX, chunkZ)) {
                return false;
            }

            log.info("Chunk ({}, {}) is being ticked.", chunkX, chunkZ);
            return true;
        });
    }

    // todos os 30 chunks ao redor do chunk do spawn (0,0) não devem ser tickados!
    private static boolean isNearbyChunkSpawn(int chunkX, int chunkZ) {
        return chunkX >= -50 && chunkX <= 50 && chunkZ >= -50 && chunkZ <= 50;
    }
}
