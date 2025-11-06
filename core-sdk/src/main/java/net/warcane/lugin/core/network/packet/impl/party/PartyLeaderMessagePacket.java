package net.warcane.lugin.core.network.packet.impl.party;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record PartyLeaderMessagePacket(
   @JsonProperty("ln") UUID leaderUUID,
    String message
) implements NetworkPacket { }
