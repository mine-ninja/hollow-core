package io.github.minehollow.event;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public interface ChunkPreTickHandler extends Comparable<ChunkPreTickHandler> {

    boolean shouldChunkTick(@NotNull World world, int chunkX, int chunkZ);

    default int priority() {
        return 100;
    }

    @Override
    default int compareTo(@NotNull ChunkPreTickHandler other) {
        int res = Integer.compare(this.priority(), other.priority());
        return res == 0 ? Integer.compare(this.hashCode(), other.hashCode()) : res;
    }
}
