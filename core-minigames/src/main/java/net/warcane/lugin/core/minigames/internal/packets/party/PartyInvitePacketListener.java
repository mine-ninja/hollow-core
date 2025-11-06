package net.warcane.lugin.core.minigames.internal.packets.party;

import net.warcane.lugin.core.minigames.MinigamesPlatformPlugin;
import net.warcane.lugin.core.network.packet.impl.party.PartyInvitePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class PartyInvitePacketListener implements PacketListener<PartyInvitePacket> {
    @Override
    public void onReceivePacket(@NotNull PartyInvitePacket packet, @NotNull Headers headers) {
        var player = Bukkit.getPlayer(packet.playerId());
        if (player == null) {
            return;
        }

        MinigamesPlatformPlugin.getInstance().adventure().player(player).sendMessage(packet.getMessage());
    }
}
