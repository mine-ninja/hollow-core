/*
 * Copyright (c) 2024-2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.BLOCK_CHANGE;
import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.CHUNK_DATA;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import gg.nerdzone.prison.mining.model.area.MineArea;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition.BlockPositionImpl;
import gg.nerdzone.prison.mining.model.block.MinePaletteBlock;
import gg.nerdzone.prison.mining.packet.MiningBukkitPacketInterceptor.MineMetadata;
import gg.nerdzone.prison.mining.packet.factory.MinePacketFactory;
import gg.nerdzone.prison.mining.packet.util.MineBlockUtil;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.NonNull;
import me.lucko.helper.Services;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

public class MineBukkitPacketFactory {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

    protected static MineBukkitPacketFactory create() {
        if (Services.get(MineBukkitPacketFactory.class).isPresent()) {
            throw new IllegalStateException("MiningBukkitPacketFactory is already loaded.");
        }

        return Services.provide(MineBukkitPacketFactory.class, new MineBukkitPacketFactory());
    }

    public static void shutdown() {
        EXECUTOR_SERVICE.shutdownNow();
    }

    public void onSend(@NonNull PacketSendEvent event, @NonNull MiningBukkitPacketInterceptor.MineMetadata metadata) {
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == CHUNK_DATA) {
            this.handleChunkDataPacket(event, metadata);
            return;
        }

        if (packetType == BLOCK_CHANGE) {
            this.handleBlockChange(event, metadata);
        }

    }

    public void onReceive(@NonNull PacketReceiveEvent event, @NonNull MiningBukkitPacketInterceptor.MineMetadata metadata) {
        final PacketTypeCommon packetType = event.getPacketType();
        if (packetType == Client.PLAYER_DIGGING) {
            this.handleBlockDig(event, metadata);
        }
    }

    @Internal
    private void handleChunkDataPacket(@NonNull PacketSendEvent event, @NonNull MiningBukkitPacketInterceptor.MineMetadata metadata) {
        final WrapperPlayServerChunkData chunkData = new WrapperPlayServerChunkData(event);
        final Column column = chunkData.getColumn();
        final int chunkX = column.getX();
        final int chunkZ = column.getZ();
        if (!(MinePacketFactory.isMineColumn(chunkX, chunkZ, metadata.mine().getArea()))) {
            return;
        }

        event.setCancelled(true);

        EXECUTOR_SERVICE.execute(() -> {
            final Column newColumn = MinePacketFactory.populateColumn(column, metadata.mine().getArea());
            if (newColumn == null) {
                return;
            }

            event.getUser().sendPacketSilently(new WrapperPlayServerChunkData(newColumn, chunkData.getLightData()));
        });
    }

    private void handleBlockChange(@NonNull PacketSendEvent event, @NonNull MineMetadata metadata) {
        final WrapperPlayServerBlockChange wrapper = new WrapperPlayServerBlockChange(event);
        final Vector3i pos = wrapper.getBlockPosition();
        final MineArea area = metadata.mine().getArea();

        if (this.isMiningFloor(area, pos, 3) || this.isMiningBorder(area, pos, 2)) {
            wrapper.setBlockID(MinePacketFactory.BEDROCK);
            return;
        }

        if (this.isMiningBorder(area, pos, 1)
            || this.isMiningFloor(area, pos, 1)
            || this.isMiningFloor(area, pos, 2)) {
            wrapper.setBlockID(MinePacketFactory.AIR);
            return;
        }

        final MineArea mineArea = this.ensureMiningArea(area, pos);
        if (mineArea == null) {
            return;
        }

        final int mineBlockId = mineArea.getBlock(pos.getX(), pos.getY(), pos.getZ());
        wrapper.setBlockID(mineBlockId);
    }

    private void handleBlockDig(@NonNull PacketReceiveEvent event, @NonNull MineMetadata metadata) {
        final WrapperPlayClientPlayerDigging wrapper = new WrapperPlayClientPlayerDigging(event);
        final DiggingAction action = wrapper.getAction();
        if (action == DiggingAction.START_DIGGING || action == DiggingAction.CANCELLED_DIGGING || action == DiggingAction.FINISHED_DIGGING) {
            final User user = event.getUser();
            final Player bukkitPlayer = Bukkit.getPlayer(user.getName());
            if (bukkitPlayer == null) {
                return;
            }

            final MineArea area = this.ensureMiningArea(metadata.mine().getArea(), wrapper.getBlockPosition());
            if (area == null) {
                return;
            }

            if (bukkitPlayer instanceof CraftPlayer craftPlayer) {
                craftPlayer.getHandle().connection.ackBlockChangesUpTo(wrapper.getSequence()); // Update ack sequence
            }

            event.setCancelled(true);

            final ItemStack item = bukkitPlayer.getInventory().getItemInMainHand();
            if (!item.getType().name().endsWith("PICKAXE")) { // Ignore non-pickaxe items
                return;
            }

            final Vector3i wrapperPos = wrapper.getBlockPosition();
            final int blockId = area.getBlock(wrapperPos.getX(), wrapperPos.getY(), wrapperPos.getZ());
            if (blockId == 0) {
                return;
            }

            MineBlockUtil.handleBlockBreak(
                metadata.mine(),
                metadata.user(),
                bukkitPlayer,
                false,
                new MinePaletteBlock(new BlockPositionImpl(wrapperPos.getX(), wrapperPos.getY(), wrapperPos.getZ()), 0)
            );
        }
    }

    @ApiStatus.Internal
    private @Nullable MineArea ensureMiningArea(@Nullable MineArea area, Vector3i position) {
        return area != null && area.isInArea(position.getX(), position.getY(), position.getZ()) ? area : null;
    }

    @ApiStatus.Internal
    private boolean isMiningBorder(@Nullable MineArea area, Vector3i position, int radius) {
        return area != null && area.isInBorder(position.getX(), position.getY(), position.getZ(), radius);
    }

    @ApiStatus.Internal
    private boolean isMiningFloor(@Nullable MineArea area, Vector3i position, int radius) {
        return area != null && (area.getMinY() - radius) == position.getY();
    }
}
