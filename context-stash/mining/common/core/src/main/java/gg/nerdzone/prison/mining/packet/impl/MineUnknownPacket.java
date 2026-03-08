/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.impl;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import gg.nerdzone.prison.mining.packet.MinePacketType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class MineUnknownPacket extends MineBukkitPacket<PacketWrapper<?>> {

    public MineUnknownPacket(PacketWrapper<?> wrapper, @Nullable Player sourcePlayer) {
        super(MinePacketType.UNKNOWN, wrapper, sourcePlayer);
    }

}
