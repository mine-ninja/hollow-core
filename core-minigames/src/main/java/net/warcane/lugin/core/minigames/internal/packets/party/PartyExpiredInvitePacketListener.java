package net.warcane.lugin.core.minigames.internal.packets.party;

import net.warcane.lugin.core.network.packet.impl.party.PartyExpiredInvitePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class PartyExpiredInvitePacketListener implements PacketListener<PartyExpiredInvitePacket> {
    @Override
    public void onReceivePacket(@NotNull PartyExpiredInvitePacket packet, @NotNull Headers headers) {
        var player = Bukkit.getPlayer(packet.playerId());
        if (player == null) {
            return;
        }

        player.sendMessage(packet.message());
    }
}
