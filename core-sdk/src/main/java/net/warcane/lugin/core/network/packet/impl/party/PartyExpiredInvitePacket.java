package net.warcane.lugin.core.network.packet.impl.party;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record PartyExpiredInvitePacket(
    @JsonProperty("pid") UUID playerId,
    @JsonProperty("m") String message

) implements NetworkPacket { }
