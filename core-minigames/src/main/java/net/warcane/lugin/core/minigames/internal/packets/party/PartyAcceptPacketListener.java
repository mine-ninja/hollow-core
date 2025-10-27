package net.warcane.lugin.core.minigames.internal.packets.party;

import net.warcane.lugin.core.network.packet.impl.party.PartyAcceptPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class PartyAcceptPacketListener implements PacketListener<PartyAcceptPacket> {
    @Override
    public void onReceivePacket(@NotNull PartyAcceptPacket packet, @NotNull Headers headers) {
        var player = Bukkit.getPlayer(packet.playerId());
        if (player == null) {
            return;
        }

        player.sendMessage(packet.message());
    }
}
