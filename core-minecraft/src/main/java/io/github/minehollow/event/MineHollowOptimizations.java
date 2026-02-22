package io.github.minehollow.event;

import org.bukkit.World;

import java.util.Arrays;
import java.util.TreeSet;

// classe injetada na fork do spigot. Precisa ser essa gambiarra por
// causa que o paperweight conflita caso tentamos usar uma api custom.
public class MineHollowOptimizations {

    private static volatile ChunkPreTickHandler[] chunkPreTickHandlers = new ChunkPreTickHandler[0];

    public static void registerPreTickHandler(ChunkPreTickHandler handler) {
        TreeSet<ChunkPreTickHandler> handlersSet = new TreeSet<>(Arrays.asList(chunkPreTickHandlers));

        handlersSet.add(handler);

        chunkPreTickHandlers = handlersSet.toArray(new ChunkPreTickHandler[0]);
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public static boolean onPreTick(World world, int x, int z) {
        final ChunkPreTickHandler[] handlers = chunkPreTickHandlers;
        for (int i = 0; i < handlers.length; i++) {
            if (!handlers[i].shouldChunkTick(world, x, z)) {
                return false;
            }
        }
        return true;
    }
}
