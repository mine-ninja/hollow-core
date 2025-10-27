package net.warcane.lugin.core.network.packet.impl.party;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record PartyDenyPacket(
    @JsonProperty("pid") UUID playerId,
    @JsonProperty("msg") String message
) implements NetworkPacket { }
