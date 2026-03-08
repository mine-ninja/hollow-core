/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.util;

import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import gg.nerdzone.prison.mining.api.context.MineBlockBreakContext;
import gg.nerdzone.prison.mining.api.context.state.MineContextState;
import gg.nerdzone.prison.mining.api.events.block.MineBlockBreakEvent;
import gg.nerdzone.prison.mining.api.events.block.MinePostBlockBreakEvent;
import gg.nerdzone.prison.mining.api.events.player.MinePlayerEvent;
import gg.nerdzone.prison.mining.context.model.MineBlockBreakContextImpl;
import gg.nerdzone.prison.mining.enums.MineResetReason;
import gg.nerdzone.prison.mining.impl.MineSkinServiceImpl;
import gg.nerdzone.prison.mining.model.area.MineArea;
import gg.nerdzone.prison.mining.model.block.MinePaletteBlock;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.model.user.MiningUser;
import gg.nerdzone.prison.mining.packet.factory.MinePacketFactory;
import gg.nerdzone.prison.mining.packet.factory.packet.MinePacketChunkUtil;
import gg.nerdzone.prison.mining.packet.impl.MineBlockChangePacket;
import gg.nerdzone.prison.mining.packet.impl.MineChunkPacket;
import gg.nerdzone.prison.mining.packet.impl.MineMultiBlockBreakPacket;
import gg.nerdzone.prison.mining.services.MineService;
import gg.nerdzone.prison.mining.services.MiningPacketService;
import gg.nerdzone.prison.model.PrisonUserProfile;
import gg.nerdzone.prison.service.PrisonUserProfileService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import me.lucko.helper.Events;
import me.lucko.helper.Services;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class MineBlockUtil {

    private MiningPacketService packetService;

    private MineSkinServiceImpl skinService;

    private PrisonUserProfileService profileService;

    /**
     * Handles and applies block changes to the mine.
     *
     * @param player The player who made the change (null if not applicable)
     * @param mine   The mine to apply the changes to
     * @param blocks The blocks to change
     */
    public void handleBlockChange(
        @Nullable Player player,
        @NotNull Mine mine,
        @NotNull MinePaletteBlock... blocks
    ) {
        final int length = blocks.length;
        if (length == 0) {
            return; // No blocks to change
        }

        if (length <= 250) {
            performSingleBlockChange(mine, player, blocks);
        } else if (length <= 20000) {
            performSectionChange(mine, player, blocks);
        } else {
            final World world = (player != null) ? player.getWorld() : getSkinService().findWorld(mine.getTheme());

            performChunkChange(mine, player, world, blocks);
        }

        mine.getArea().set(blocks);
    }

    /**
     * Handles the block break event.
     *
     * @param event  The event to handle
     * @param blocks The blocks to break
     */
    public void handleBlockBreak(@NonNull MineBlockBreakEvent event, @NonNull MinePaletteBlock... blocks) {
        handleBlockBreak(event, false, blocks);
    }

    /**
     * Handles the block break event.
     *
     * @param event  The event to handle
     * @param silent If the break should be call events, if false, the event will be called
     * @param blocks The blocks to break
     */
    public void handleBlockBreak(@NonNull MineBlockBreakEvent event, boolean silent, @NonNull MinePaletteBlock... blocks) {
        handleBlockBreak(event.getMine(), event.getMiningUser(), event.getPlayer(), silent, blocks);
    }

    /**
     * Handles the block break event.
     *
     * @param mine   The mine to break blocks in
     * @param source The player who broke the block
     * @param silent If the break should be call events, if false, the event will be called
     * @param blocks The blocks to break
     */
    public void handleBlockBreak(
        @NonNull Mine mine,
        @NonNull MiningUser miningUser,
        @Nullable Player source,
        boolean silent,
        @NonNull MinePaletteBlock... blocks
    ) {
        if (mine.isResetting()) {
            return; // Ignore if the mine is resetting
        }

        if (!silent) {
            final MinePlayerEvent playerEvent = MineEventDispatcher.dispatchBlockBreak(mine, source, blocks);
            if (playerEvent instanceof MineBlockBreakEvent event) {
                final MiningUser user = event.getMiningUser();
                final PrisonUserProfile profile = Objects.requireNonNull(getProfileService().addExperience(
                    user.getName(),
                    event.getExperience() * 5
                ));

                final MineBlockBreakContextImpl context = MineBlockBreakContextImpl.create(
                    event.getPosition(),
                    event.getPreviousMaterial(),
                    mine,
                    user,
                    profile.getLevel()
                );

                if (context.isMultiBlock()) {
                    Arrays.asList(blocks)
                        .forEach(block -> context.addBlock(block, MineBlockBreakContext.BlockInsertRule.SKIP_IF_AIR));
                }

                context.getState().whenComplete(
                    "post-event",
                    MineContextState.StatePriority.MONITOR, ($) -> Events.call(new MinePostBlockBreakEvent(source, mine, event.getReason(), blocks))
                );

                context.dispatch();
            }
            return;
        }

        handleBlockChange(source, mine, blocks);

        // tryPerformReset(source, mine); // Try to reset the mine if needed
    }

    @ApiStatus.Internal
    private void performSingleBlockChange(Mine mine, Player player, MinePaletteBlock... result) {
        final MiningPacketService packetService = getPacketService();
        for (final MinePaletteBlock block : result) {
            if (block == null) {
                continue; // Skip null blocks
            }

            final MineBlockChangePacket packet = new MineBlockChangePacket(player, block.position(), block.id());
            packetService.sendMinePacket(player, mine, packet, true);
        }
    }

    @Internal
    private MiningPacketService getPacketService() {
        if (packetService != null) {
            return packetService;
        }

        return packetService = Services.load(MiningPacketService.class);
    }

    @Internal
    private MineSkinServiceImpl getSkinService() {
        if (skinService != null) {
            return skinService;
        }

        return skinService = Services.load(MineSkinServiceImpl.class);
    }

    @Internal
    private PrisonUserProfileService getProfileService() {
        if (profileService != null) {
            return profileService;
        }

        return profileService = PrisonUserProfileService.get();
    }

    @ApiStatus.Internal
    private void performSectionChange(Mine mine, Player player, MinePaletteBlock... result) {
        final MiningPacketService packetService = getPacketService();
        final MapChunk chunkBlocks = createChunkMap(result);
        final Map<Vector3i, WrapperPlayServerMultiBlockChange.EncodedBlock[]> blocksMap = chunkBlocks.toBlock();

        for (final Map.Entry<Vector3i, WrapperPlayServerMultiBlockChange.EncodedBlock[]> entry : blocksMap.entrySet()) {
            final Vector3i chunkPos = entry.getKey();
            final WrapperPlayServerMultiBlockChange.EncodedBlock[] encodedBlocks = entry.getValue();
            packetService.sendMinePacket(player, mine, new MineMultiBlockBreakPacket(player, chunkPos, encodedBlocks), true);
        }
    }

    @ApiStatus.Internal
    private @NotNull MapChunk createChunkMap(MinePaletteBlock... result) {
        final MapChunk mapChunk = new MapChunk();

        for (final MinePaletteBlock block : result) {
            if (block == null) {
                continue; // Skip null blocks
            }

            final ChunkBlock chunk = mapChunk.computeIfAbsent(new ChunkSectionPos(block), current -> new ChunkBlock(new ArrayList<>()));
            chunk.setBlock(block.x(), block.y(), block.z(), block.id());
        }

        return mapChunk;
    }

    @ApiStatus.Internal
    private void performChunkChange(Mine mine, Player player, World world, MinePaletteBlock... result) {
        final MiningPacketService packetService = getPacketService();
        final Map<ColumnPos, List<MinePaletteBlock>> blocksByColumn = new LinkedHashMap<>();

        for (final MinePaletteBlock block : result) {
            if (block == null) {
                continue;
            }

            final ColumnPos pos = new ColumnPos(block.getChunkX(), block.getChunkZ());
            blocksByColumn.computeIfAbsent(pos, k -> new ArrayList<>()).add(block);
        }

        for (final Map.Entry<ColumnPos, List<MinePaletteBlock>> entry : blocksByColumn.entrySet()) {
            final ColumnPos pos = entry.getKey();
            final List<MinePaletteBlock> blocksInColumn = entry.getValue();
            final Column column = MinePacketFactory.getColumn(world, pos.x, pos.z, mine, false);
            final ArrayList<WrapperPlayServerMultiBlockChange.EncodedBlock> encodedBlocks = new ArrayList<>();
            for (final MinePaletteBlock b : blocksInColumn) {
                encodedBlocks.add(new WrapperPlayServerMultiBlockChange.EncodedBlock(b.id(), b.x(), b.y(), b.z()));
            }

            final Column finalColumn = MinePacketChunkUtil.applyAirChanges(
                encodedBlocks,
                pos.x,
                pos.z,
                column.getChunks()
            );
            packetService.sendMinePacket(player, mine, new MineChunkPacket(finalColumn, player), true);
        }
    }

    @Internal
    private void tryPerformReset(@Nullable Player player, @NonNull Mine mine) {
        if (mine.isResetting()) { // Skip if the mine is already resetting
            return;
        }

        final MineArea area = mine.getArea();
        final int totalContent = area.getVolume();
        final int currentContent = area.getCurrentVolume();
        final double remainingPercentage = (currentContent / (double) totalContent) * 100.0D;
        final double minedPercentage = 100.0D - remainingPercentage;

        if (minedPercentage < mine.getResetPercentage()) {
            return;
        }

        final MineService mineService = Services.load(MineService.class);
        mineService.reset(mine, null, MineResetReason.AUTOMATIC);
    }

    private record ChunkBlock(ArrayList<WrapperPlayServerMultiBlockChange.EncodedBlock> blocks) {

        public void setBlock(int x, int y, int z, int id) {
            this.blocks.add(new WrapperPlayServerMultiBlockChange.EncodedBlock(id, x, y, z));
        }

        public @NotNull WrapperPlayServerMultiBlockChange.EncodedBlock[] getBlocks() {
            return this.blocks.toArray(new WrapperPlayServerMultiBlockChange.EncodedBlock[0]);
        }
    }

    private record ColumnPos(int x, int z) {}

    private record ChunkSectionPos(int x, int y, int z) {
        ChunkSectionPos(MinePaletteBlock block) {
            this(block.getChunkX(), block.getChunkY(), block.getChunkZ());
        }

        Vector3i toVector3i() {
            return new Vector3i(this.x, this.y, this.z);
        }
    }

    private static class MapChunk extends LinkedHashMap<ChunkSectionPos, ChunkBlock> {

        public @NotNull Map<Vector3i, WrapperPlayServerMultiBlockChange.EncodedBlock[]> toBlock() {
            return this.entrySet()
                .stream()
                .collect(
                    LinkedHashMap::new,
                    (map, entry) -> map.put(entry.getKey().toVector3i(), entry.getValue().getBlocks()),
                    LinkedHashMap::putAll
                );
        }
    }
}
