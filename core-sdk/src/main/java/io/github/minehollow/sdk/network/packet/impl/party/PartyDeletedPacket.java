package io.github.minehollow.sdk.network.packet.impl.party;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.List;

public record PartyDeletedPacket(
    @JsonProperty("mn") List<String> memberNames,
    @JsonProperty("ms") String message
) implements NetworkPacket { }
