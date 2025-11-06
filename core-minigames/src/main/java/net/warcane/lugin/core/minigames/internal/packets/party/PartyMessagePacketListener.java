package net.warcane.lugin.core.minigames.internal.packets.party;

import net.warcane.lugin.core.minigames.MinigamesPlatform;
import net.warcane.lugin.core.network.packet.impl.party.PartyMessagePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.jetbrains.annotations.NotNull;

public record PartyMessagePacketListener(MinigamesPlatform platform) implements PacketListener<PartyMessagePacket> {
    @Override
    public void onReceivePacket(@NotNull PartyMessagePacket packet, @NotNull Headers headers) {
        platform.getPartyService().sendPartyChatMessageToMember(packet.partyId(), packet.getMessage(), packet.excludeLeader());
    }
}
