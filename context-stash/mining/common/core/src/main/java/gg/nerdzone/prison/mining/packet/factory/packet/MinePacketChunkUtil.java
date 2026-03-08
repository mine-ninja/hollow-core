/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.factory.packet;

import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.impl.v_1_18.Chunk_v1_18;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.DataPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.ListPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.MapPalette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.Palette;
import com.github.retrooper.packetevents.protocol.world.chunk.palette.PaletteType;
import com.github.retrooper.packetevents.protocol.world.chunk.storage.BaseStorage;
import com.github.retrooper.packetevents.protocol.world.chunk.storage.BitStorage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange.EncodedBlock;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import gg.nerdzone.prison.mining.packet.factory.wrapper.ListPaletteWrapper;
import gg.nerdzone.prison.mining.packet.factory.wrapper.MapPaletteWrapper;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for chunk packet creation.
 */
@UtilityClass
public class MinePacketChunkUtil {

    private final EnumMap<Material, Integer> BLOCK_ID_CACHE = new EnumMap<>(Material.class);
    private final ConcurrentHashMap<CacheKey, CachedBaseChunk> CACHED_CHUNKS = new ConcurrentHashMap<>();

    /**
     * Deeply clones the chunk with the given changes. Note that this method does not implement light data; it only populates packet data with block changes.
     * <p>
     * Light data population is the responsibility of the mine packet to prevent client crashes.
     *
     * @param levelChunk The chunk to clone
     * @param changes    The changes to apply
     * @return The cloned column chunk
     * @implNote Some code is borrowed by Kooperlol. Thanks to the author for the base code.
     */
    @SuppressWarnings("all")
    public Column deepCloneChunk(@NotNull String ownerId, @NotNull LevelChunk levelChunk, @NotNull Long2IntMap changes) {
        final String worldName = levelChunk.getLevel().getWorld().getName();
        final long chunkHash = levelChunk.getPos().longKey;

        final CachedBaseChunk cachedChunk = CACHED_CHUNKS.get(new CacheKey(worldName, chunkHash));
        if (cachedChunk != null && !(cachedChunk.isExpired())) {
            return cachedChunk.applyChanges(changes, ownerId);
        }

        final CraftChunk chunk = new CraftChunk(levelChunk);
        final int ySection = levelChunk.getLevel().getHeight() >> 4;
        final List<BaseChunk> chunks = new ArrayList<>(ySection);
        final ChunkSnapshot chunkSnapshot = chunk.getChunkSnapshot();

        final int maxHeight = chunk.getWorld().getMaxHeight();
        final int minHeight = chunk.getWorld().getMinHeight();

        // Normalize X/Z
        final int chunkX = levelChunk.getPos().x << 4;
        final int chunkZ = levelChunk.getPos().z << 4;

        final List<Chunk_v1_18> cachedChunks = new ArrayList<>(ySection);

        for (int section = 0; section < ySection; section++) {
            final Chunk_v1_18 chunkSection = new Chunk_v1_18();
            final Chunk_v1_18 cacheSection = new Chunk_v1_18();// Used only to save default block states that will be cached.

            final long baseY = ((long) section << 4) + minHeight;
            for (int y = 0; y < 16; y++) {
                final long worldY = baseY + y;
                if (worldY < minHeight || worldY >= maxHeight) {
                    continue;
                }

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        final int worldX = chunkX + x;
                        final int worldZ = chunkZ + z;
                        final long blockPosKey = MineBlockPosition.asLong(worldX, (int) worldY, worldZ);

                        // Real block data
                        final Material defType = chunkSnapshot.getBlockType(x, (int) worldY, z);
                        final int changeId = changes.get(blockPosKey);
                        final int realId = BLOCK_ID_CACHE.computeIfAbsent(
                            defType,
                            material -> Block.getId(((CraftBlockState) defType.createBlockData().createBlockState()).getHandle())
                        );

                        chunkSection.set(x, y, z, changeId != -1 ? changeId : realId);
                        cacheSection.set(x, y, z, realId);
                    }
                }
            }

            // Set biome data
            final int biomeId = chunkSection.getBiomeData().palette.stateToId(1);
            final int cachedBiomeId = cacheSection.getBiomeData().palette.stateToId(1);
            Arrays.fill(chunkSection.getBiomeData().storage.getData(), biomeId);
            Arrays.fill(cacheSection.getBiomeData().storage.getData(), cachedBiomeId);

            chunks.add(chunkSection);
            cachedChunks.add(cacheSection);
        }

        // Save the default chunk for future use.
        CACHED_CHUNKS.put(
            new CacheKey(worldName, chunkHash), new CachedBaseChunk(chunk.getX(), chunk.getZ(), cachedChunks.toArray(new Chunk_v1_18[0])));

        return new Column(chunk.getX(), chunk.getZ(), false, chunks.toArray(new BaseChunk[ySection]), null);
    }

    public Column applyChanges(Long2IntMap changes, int chunkX, int chunkZ, BaseChunk[] clonedChunks) {
        final Map<Integer, List<Entry>> bySection = new HashMap<>();

        for (final Entry entry : changes.long2IntEntrySet()) {
            final int wx = MineBlockPosition.getPackedX(entry.getLongKey());
            final int wy = MineBlockPosition.getPackedY(entry.getLongKey());
            final int wz = MineBlockPosition.getPackedZ(entry.getLongKey());
            if (wx >> 4 != chunkX || wz >> 4 != chunkZ) {
                continue;
            }

            final int section = (wy >> 4) + 4;
            if (section >= 0 && section < clonedChunks.length) {
                bySection.computeIfAbsent(section, $ -> new ArrayList<>()).add(entry);
            }
        }

        bySection.forEach((section, entries) -> {
            final BaseChunk chunk = clonedChunks[section];
            for (final Entry entry : entries) {
                final int x = MineBlockPosition.getPackedX(entry.getLongKey()) & 0xF;
                final int y = MineBlockPosition.getPackedY(entry.getLongKey()) & 0xF;
                final int z = MineBlockPosition.getPackedZ(entry.getLongKey()) & 0xF;
                chunk.set(x, y, z, entry.getIntValue());
            }
        });

        return new Column(chunkX, chunkZ, false, clonedChunks, null);
    }

    public Column applyAirChanges(ArrayList<EncodedBlock> changes, int chunkX, int chunkZ, BaseChunk[] clonedChunks) {
        for (final EncodedBlock change : changes) {
            if ((change.getX() >> 4) != chunkX || (change.getZ() >> 4) != chunkZ) {
                continue;
            }

            final int section = (change.getY() >> 4) + 4;
            if (section >= 0 && section < clonedChunks.length) {
                final BaseChunk chunk = clonedChunks[section];
                chunk.set(change.getX() & 0xF, change.getY() & 0xF, change.getZ() & 0xF, 0);
            }
        }

        return new Column(chunkX, chunkZ, false, clonedChunks, null);
    }

    @Slf4j
    public static class CachedBaseChunk {

        private final int chunkX, chunkZ;
        private final BaseChunk[] sections;

        // Expire after 3 min to keep sync with the server world and prevent potential memory leaks.
        private final long expireAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3);

        protected CachedBaseChunk(int chunkX, int chunkZ, BaseChunk[] sections) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.sections = sections;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= this.expireAt;
        }

        public Column applyChanges(Long2IntMap changes, String ownerId) {
            return MinePacketChunkUtil.applyChanges(changes, this.chunkX, this.chunkZ, this.copy(ownerId));
        }

        private Chunk_v1_18[] copy(String ownerId) {
            final Chunk_v1_18[] copied = new Chunk_v1_18[this.sections.length];

            // Deep copy
            for (int i = 0; i < this.sections.length; i++) {
                if (this.sections[i] instanceof Chunk_v1_18 chunk) {
                    final int blockCount = chunk.getBlockCount();
                    final DataPalette chunkPalette = this.copyPalette(ownerId, this.chunkX, this.chunkZ, chunk.getChunkData(), true);
                    final DataPalette biomePalette = this.copyPalette(ownerId, this.chunkX, this.chunkZ, chunk.getBiomeData(), false);

                    copied[i] = new Chunk_v1_18(blockCount, chunkPalette, biomePalette);
                } else {
                    throw new IllegalStateException("Invalid chunk type: " + this.sections[i].getClass().getName()); // Version impl issue?
                }
            }

            return copied;
        }

        /**
         * Deeply copies the chunk palette.
         *
         * @param dataPalette The palette to copy
         * @return The copied palette
         */
        private @NotNull DataPalette copyPalette(String ownerId, int chunkX, int chunkZ, DataPalette dataPalette, boolean blockPalette) {
            final Palette palette = dataPalette.palette;
            final BaseStorage storage = dataPalette.storage;
            final PaletteType paletteType = dataPalette.paletteType;

            // List palette is used for chunks with blocks.
            // For example, a chunk with only air blocks will use the single palette format.
            if (palette instanceof ListPalette listPalette) {
                final ListPaletteWrapper wrapper = MinePaletteUtil.deepCopy(listPalette);

                // Same here, BitStorage is used for chunks with blocks.
                // Empty chunks don't have a BitStorage.
                final BaseStorage copyStorage = (storage instanceof BitStorage b) ? MineBitStorageUtil.deepCopy(b) : storage;

                if (blockPalette && !(storage instanceof BitStorage)) {
                    log.warn("(List palette) Unsupported storage type: {} ({} {} > {})", palette.getClass().getName(), chunkX, chunkZ, ownerId);
                }

                return new DataPalette(wrapper, copyStorage, paletteType);
            } else if (palette instanceof MapPalette mapPalette) {
                final MapPaletteWrapper wrapper = MinePaletteUtil.deepCopyMapPalette(mapPalette);
                final BaseStorage copyStorage = (storage instanceof BitStorage b) ? MineBitStorageUtil.deepCopy(b) : storage;
                if (blockPalette && !(storage instanceof BitStorage)) {
                    log.warn("(Map Palette) Unsupported storage type: {} ({} {} > {})", palette.getClass().getName(), chunkX, chunkZ, ownerId);
                }

                return new DataPalette(wrapper, copyStorage, paletteType);
            } else if (blockPalette) {
                log.warn("Unsupported palette type: {} ({} {} > {})", palette.getClass().getName(), chunkX, chunkZ, ownerId);
            }

            return dataPalette;
        }
    }

    private record CacheKey(String worldName, long chunkHash) {}
}
