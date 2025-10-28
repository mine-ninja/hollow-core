package net.warcane.lugin.core.minigames.internal.packets.party;

import net.warcane.lugin.core.network.packet.impl.party.PartyLeaderMessagePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class PartyLeaderMessagePacketListener implements PacketListener<PartyLeaderMessagePacket> {
    @Override
    public void onReceivePacket(@NotNull PartyLeaderMessagePacket packet, @NotNull Headers headers) {
        var player = Bukkit.getPlayer(packet.leaderUUID());
        if (player != null) {
            player.sendMessage(packet.message());
        }
    }
}
