/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.impl;

import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition.BlockPositionImpl;
import gg.nerdzone.prison.mining.packet.MinePacket;
import gg.nerdzone.prison.mining.packet.MinePacketType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a block change packet.
 *
 * @see WrapperPlayServerBlockChange
 * @see MinePacket
 */
public class MineBlockChangePacket extends MineBukkitPacket<WrapperPlayServerBlockChange> {

    public MineBlockChangePacket(@Nullable Player player, @NotNull WrapperPlayServerBlockChange wrapper) {
        super(MinePacketType.BLOCK_CHANGE, wrapper, player);
    }

    public MineBlockChangePacket(@Nullable Player player, @NotNull MineBlockPosition pos, int blockId) {
        super(MinePacketType.BLOCK_CHANGE, new WrapperPlayServerBlockChange(new Vector3i(pos.x(), pos.y(), pos.z()), blockId), player);
    }

    protected MineBlockChangePacket(@NotNull MinePacketType packetType, @NotNull WrapperPlayServerBlockChange wrapper, @Nullable Player player) {
        super(packetType, wrapper, player);
    }

    public @NotNull MineBlockPosition getBlockPos() {
        return new BlockPositionImpl(
            this.getWrapper().getBlockPosition().getX(), this.getWrapper().getBlockPosition().getY(), this.getWrapper().getBlockPosition().getZ());
    }

    @Override
    public String toString() {
        final MineBlockPosition blockPos = this.getBlockPos();
        return blockPos.x() + ", " + blockPos.y() + ", " + blockPos.z() + " -> " + this.getWrapper().getBlockId();
    }
}
