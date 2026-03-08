/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.impl;

import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import gg.nerdzone.prison.mining.api.events.MineEvent;
import gg.nerdzone.prison.mining.api.events.block.MineBlockBreakEvent;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition.BlockPositionImpl;
import gg.nerdzone.prison.mining.packet.MinePacket;
import gg.nerdzone.prison.mining.packet.MinePacketType;
import gg.nerdzone.prison.mining.util.MineMaterialUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a block break packet.
 *
 * @implNote This packet is sent when a block is broken.
 * @see MinePacket
 * @see MineBlockChangePacket
 */
public class MineBlockBreakPacket extends MineBlockChangePacket {

    public static final int AIR = StateTypes.AIR.createBlockState().getGlobalId();

    public MineBlockBreakPacket(@Nullable Player player, @NotNull WrapperPlayServerBlockChange wrapper) {
        this(player, new BlockPositionImpl(wrapper.getBlockPosition().getX(), wrapper.getBlockPosition().getY(), wrapper.getBlockPosition().getZ()));
    }

    public MineBlockBreakPacket(@Nullable Player player, @NotNull MineBlockPosition position) {
        super(MinePacketType.BLOCK_BREAK, new WrapperPlayServerBlockChange(new Vector3i(position.x(), position.y(), position.z()), AIR), player);
    }

    @Override
    public void adapt(@NotNull MineEvent event) {
        if (event instanceof MineBlockBreakEvent breakEvent) {
            final MineBlockPosition position = breakEvent.getPosition();
            final Material blockId = breakEvent.getCurrentMaterial();

            this.getWrapper().setBlockPosition(new Vector3i(position.x(), position.y(), position.z()));
            this.getWrapper().setBlockID(MineMaterialUtil.fromMaterial(blockId));
        }
    }
}
