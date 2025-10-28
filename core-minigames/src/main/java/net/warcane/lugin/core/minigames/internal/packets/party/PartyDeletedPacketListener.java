package net.warcane.lugin.core.minigames.internal.packets.party;

import net.warcane.lugin.core.network.packet.impl.party.PartyDeletedPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class PartyDeletedPacketListener implements PacketListener<PartyDeletedPacket> {
    @Override
    public void onReceivePacket(@NotNull PartyDeletedPacket packet, @NotNull Headers headers) {
        for (var name : packet.memberNames()) {
            var player = Bukkit.getPlayer(name);
            if (player != null) {
                player.sendMessage(packet.message());
            }
        }
    }
}
