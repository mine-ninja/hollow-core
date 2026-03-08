/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.registry;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import gg.nerdzone.prison.mining.packet.MinePacket;
import gg.nerdzone.prison.mining.packet.MinePacketType;
import gg.nerdzone.prison.mining.packet.impl.MineBlockBreakPacket;
import gg.nerdzone.prison.mining.packet.impl.MineMultiBlockBreakPacket;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Registry for mine packets.
 *
 * @implNote This class can use reflection in the future to automatically register packets.
 */
@UtilityClass
public class MinePacketRegistry {

    private final Map<MinePacketType, Class<? extends PacketWrapper<?>>> PACKET_MAP = Map.of(
        MinePacketType.BLOCK_BREAK, WrapperPlayServerBlockChange.class,
        MinePacketType.MULTI_BLOCK_BREAK, WrapperPlayServerMultiBlockChange.class
    );

    private final Map<MinePacketType, BiFunction<Player, PacketWrapper<?>, MinePacket<?>>> FACTORY_MAP = Map.of(
        MinePacketType.BLOCK_BREAK, (player, wrapper) -> new MineBlockBreakPacket(player, (WrapperPlayServerBlockChange) wrapper),
        MinePacketType.MULTI_BLOCK_BREAK, (player, wrapper) -> new MineMultiBlockBreakPacket(player, (WrapperPlayServerMultiBlockChange) wrapper)
    );

    /**
     * Create a packet from a wrapper
     *
     * @param source  The player that sent the packet
     * @param wrapper The wrapper packet
     * @return The mine packet
     */
    public @Nullable MinePacket<?> createPacket(@Nullable Player source, @NotNull PacketWrapper<?> wrapper) {
        return PACKET_MAP.entrySet()
            .stream()
            .filter(entry -> entry.getValue().isInstance(wrapper))
            .findFirst()
            .map(entry -> FACTORY_MAP.get(entry.getKey()).apply(source, wrapper))
            .orElse(null);
    }
}
