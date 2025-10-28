package net.warcane.lugin.core.minigames.internal.packets;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minigames.MinigamesPlatform;
import net.warcane.lugin.core.minigames.internal.packets.party.*;
import net.warcane.lugin.core.network.packet.impl.party.*;

@Slf4j
public record InternalPacketListeners(MinigamesPlatform platform) {
    public void setup() {
        final var networkClient = platform.getNetworkClient();
        networkClient.registerPacketListener(PartyExpiredInvitePacket.class, new PartyExpiredInvitePacketListener());
        networkClient.registerPacketListener(PartyInvitePacket.class, new PartyInvitePacketListener());
        networkClient.registerPacketListener(PartyMessagePacket.class, new PartyMessagePacketListener(platform));
        networkClient.registerPacketListener(PartyLeaderMessagePacket.class, new PartyLeaderMessagePacketListener());
        networkClient.registerPacketListener(PartyDeletedPacket.class, new PartyDeletedPacketListener());
    }
}
