/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.impl;

import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange.EncodedBlock;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition.BlockPositionImpl;
import gg.nerdzone.prison.mining.packet.MinePacketType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class MineMultiBlockBreakPacket extends MineBukkitPacket<WrapperPlayServerMultiBlockChange> {

    public MineMultiBlockBreakPacket(@Nullable Player player, WrapperPlayServerMultiBlockChange wrapper) {
        super(MinePacketType.MULTI_BLOCK_BREAK, wrapper, player);
    }

    public MineMultiBlockBreakPacket(@Nullable Player player, @NotNull Vector3i chunkPos, @NotNull EncodedBlock[] blockData) {
        super(MinePacketType.MULTI_BLOCK_BREAK, new WrapperPlayServerMultiBlockChange(chunkPos, false, blockData), player);
    }

    public Set<MineBlockPosition> getBlocksPos() {
        return Arrays.stream(this.getWrapper().getBlocks())
            .map(block -> new BlockPositionImpl(block.getX(), block.getY(), block.getZ()))
            .collect(Collectors.toSet());
    }
}
