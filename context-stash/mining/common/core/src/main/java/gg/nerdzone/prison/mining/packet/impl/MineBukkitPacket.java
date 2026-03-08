/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.impl;

import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import gg.nerdzone.prison.mining.api.events.MineEvent;
import gg.nerdzone.prison.mining.packet.MinePacket;
import gg.nerdzone.prison.mining.packet.MinePacketType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MineBukkitPacket<T> extends MinePacket<T> {

    public MineBukkitPacket(MinePacketType type, T wrapper, @Nullable Player sourcePlayer) {
        super(type, wrapper, sourcePlayer);
    }

    /**
     * Apply event changes if needed. (e.g., block break event can change the block id)
     *
     * @param event The event to adapt
     */
    public void adapt(@NotNull MineEvent event) {
        // Adapt the event to the packet
    }

    public @NotNull PacketWrapper<?> wrapper() {
        return (PacketWrapper<?>) this.getWrapper();
    }
}
