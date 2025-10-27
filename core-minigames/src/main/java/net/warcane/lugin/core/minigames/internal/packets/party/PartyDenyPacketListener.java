package net.warcane.lugin.core.minigames.internal.packets.party;

import net.warcane.lugin.core.network.packet.impl.party.PartyDenyPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class PartyDenyPacketListener implements PacketListener<PartyDenyPacket> {
    @Override
    public void onReceivePacket(@NotNull PartyDenyPacket packet, @NotNull Headers headers) {
        var player = Bukkit.getPlayer(packet.playerId());
        if (player == null) {
            return;
        }

        player.sendMessage(packet.message());
    }
}
