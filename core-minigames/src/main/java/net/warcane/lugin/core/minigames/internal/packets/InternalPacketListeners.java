package net.warcane.lugin.core.minigames.internal.packets;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minigames.MinigamesPlatform;
import net.warcane.lugin.core.minigames.internal.packets.party.PartyAcceptPacketListener;
import net.warcane.lugin.core.minigames.internal.packets.party.PartyDenyPacketListener;
import net.warcane.lugin.core.minigames.internal.packets.party.PartyExpiredInvitePacketListener;
import net.warcane.lugin.core.minigames.internal.packets.party.PartyInvitePacketListener;
import net.warcane.lugin.core.network.packet.impl.party.PartyAcceptPacket;
import net.warcane.lugin.core.network.packet.impl.party.PartyDenyPacket;
import net.warcane.lugin.core.network.packet.impl.party.PartyExpiredInvitePacket;
import net.warcane.lugin.core.network.packet.impl.party.PartyInvitePacket;

@Slf4j
public record InternalPacketListeners(MinigamesPlatform platform) {
    public void setup() {
        final var networkClient = platform.getNetworkClient();
        networkClient.registerPacketListener(PartyExpiredInvitePacket.class, new PartyExpiredInvitePacketListener());
        networkClient.registerPacketListener(PartyInvitePacket.class, new PartyInvitePacketListener());
        networkClient.registerPacketListener(PartyAcceptPacket.class, new PartyAcceptPacketListener());
        networkClient.registerPacketListener(PartyDenyPacket.class, new PartyDenyPacketListener());
    }
}
