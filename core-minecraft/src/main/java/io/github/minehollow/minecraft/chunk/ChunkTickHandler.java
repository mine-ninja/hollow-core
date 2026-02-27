package io.github.minehollow.minecraft.chunk;

import io.github.minehollow.event.ChunkPreTickHandler;
import io.github.minehollow.event.MineHollowOptimizations;
import io.github.minehollow.minecraft.misc.chunk.ChunkPlayerCountCache;
import io.github.minehollow.sdk.util.property.Property;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;

public final class ChunkTickHandler {


    private static final boolean OPTIMIZE_CHUNK_TICKING = Property.getBoolean("minehollow.optimizations.optimizeChunkTicking", true);
    private static final int SPAWN_CHUNK_RADIUS = Optional.of(
            Property.get("minehollow.optimizations.spawnChunkRadius", "50")
        )
        .map(Integer::parseInt)
        .orElse(50);



    public static void register() {
        MineHollowOptimizations.registerPreTickHandler(new ChunkPreTickHandler() {

            @Override
            public int priority() {
                return 0;
            }

            @Override
            public boolean shouldChunkTick(@NotNull World world, int chunkX, int chunkZ) {
                if (!OPTIMIZE_CHUNK_TICKING) {
                    return true;
                }

                if (Math.abs(chunkX) <= SPAWN_CHUNK_RADIUS && Math.abs(chunkZ) <= SPAWN_CHUNK_RADIUS) {
                    return true;
                }

                return ChunkPlayerCountCache.isChunkActive(((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32));
            }
        });
    }
}