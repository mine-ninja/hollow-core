/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.factory;

import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import gg.nerdzone.prison.mining.model.area.MineArea;
import gg.nerdzone.prison.mining.model.area.MiningAreaChunk;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.packet.MinePacket;
import gg.nerdzone.prison.mining.packet.factory.packet.MinePacketChunkUtil;
import gg.nerdzone.prison.mining.packet.impl.MineChunkPacket;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Factory class for creating mine packets.
 *
 * @see MinePacketChunkUtil
 */
@UtilityClass
public class MinePacketFactory {
    public final int BEDROCK = StateTypes.BEDROCK.createBlockState().getGlobalId();
    public final int AIR = 0;

    @Contract("_, _, _ -> new")
    public @NotNull List<MinePacket<?>> createResetPackets(World world, Mine mine, boolean recreateChunks) {
        final ArrayList<MinePacket<?>> packets = new ArrayList<>();

        // Populate packets
        for (final ChunkPos chunkPos : createChunks(world, mine, recreateChunks)) {
            packets.add(new MineChunkPacket(chunkPos.column, null));
        }

        return packets;
    }

    public boolean isMineColumn(int chunkX, int chunkZ, MineArea mineArea) {
        return mineArea.isChunkInArea(chunkX, chunkZ, 2);
    }

    public @Nullable Column populateColumn(Column column, MineArea mineArea) {
        final int chunkX = column.getX();
        final int chunkZ = column.getZ();
        if (!isMineColumn(chunkX, chunkZ, mineArea)) { // Safe double-check
            return null;
        }

        final Long2IntMap chunkChanges = createChunkChanges(mineArea, new ChunkPos(chunkX, chunkZ), false);
        return MinePacketChunkUtil.applyChanges(chunkChanges, chunkX, chunkZ, column.getChunks());
    }

    private List<ChunkPos> createChunks(World world, Mine mine, boolean recreateChunks) {
        final List<ChunkPos> chunks = new ArrayList<>();

        // Populate chunks, expand 2 blocks to support borders
        mine.getArea().forEachChunks(
            2, (chunkX, chunkZ) -> { // Optimize blocks iteration
                if (chunks.contains(new ChunkPos(chunkX, chunkZ))) {
                    return;
                }

                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        );

        chunks.forEach(chunkPos -> chunkPos.column = getColumn(world, chunkPos.x, chunkPos.z, mine, recreateChunks));
        return chunks;
    }

    /**
     * Get or create a column for the given chunk position.
     *
     * @param world    The world
     * @param chunkX   The chunk x
     * @param chunkZ   The chunk z
     * @param mine     The mine
     * @param recreate If the chunk should be recreated or use the current blocks state
     * @return The column
     */
    public Column getColumn(World world, int chunkX, int chunkZ, Mine mine, boolean recreate) {
        final CraftWorld craftWorld = (CraftWorld) world;
        final Level level = craftWorld.getHandle();
        final LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        final ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        return MinePacketChunkUtil.deepCloneChunk(mine.getOwnerId(), chunk, createChunkChanges(mine.getArea(), chunkPos, recreate));
    }

    /**
     * Create the chunk changes for the given mine.
     *
     * @param mineArea The mine area
     * @param chunk    The single chunk
     * @param recreate If the chunk should be recreated or use the current blocks state
     * @return The chunk changes
     */
    private @NotNull Long2IntMap createChunkChanges(MineArea mineArea, ChunkPos chunk, boolean recreate) {
        final Long2IntMap hashBlockChanges = new Long2IntOpenHashMap();
        hashBlockChanges.defaultReturnValue(-1);

        final MineArea area = mineArea.expand(0, 3, 0); // Expand area to support borders
        final MiningAreaChunk mineChunk = area.getChunk(chunk.x, chunk.z, recreate); // Reuse chunk if already loaded
        final int minY = area.getMinY();

        area.populate(
            2, chunk.x, chunk.z, (radius, x, y, z) -> setBlock(x, y, z, minY, mineChunk, radius, hashBlockChanges)
        );

        return hashBlockChanges;
    }

    private void setBlock(int x, int y, int z, int minY, MiningAreaChunk mineChunk, int radius, Long2IntMap hashBlockChanges) {
        if (radius != 2) {
            if (y == minY + 2) {
                hashBlockChanges.put(MineBlockPosition.asLong(x, y + 2, z), AIR);
                return;
            } else if (y == minY + 1) {
                hashBlockChanges.put(MineBlockPosition.asLong(x, y + 1, z), AIR);
                return;
            } else if (y == minY) {
                hashBlockChanges.put(MineBlockPosition.asLong(x, y, z), BEDROCK);
                return;
            }
        }

        final int state = switch (radius) {
            case 0 -> mineChunk.getBlock(x & 0xF, y, z & 0xF);
            case 1 -> AIR;
            case 2 -> BEDROCK;
            default -> -1;
        };

        if (state == -1) {
            return;
        }

        hashBlockChanges.put(MineBlockPosition.asLong(x, y, z), state);
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode(exclude = "column")
    private static class ChunkPos {
        private final int x, z;

        @Setter
        private Column column;

    }
}
