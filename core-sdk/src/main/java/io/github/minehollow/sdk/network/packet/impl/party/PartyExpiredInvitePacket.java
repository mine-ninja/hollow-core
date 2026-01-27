package io.github.minehollow.sdk.network.packet.impl.party;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.UUID;

public record PartyExpiredInvitePacket(
    @JsonProperty("pid") UUID playerId,
    @JsonProperty("m") String message

) implements NetworkPacket { }
