package io.github.minehollow.sdk.network.packet.impl.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.UUID;

public record GoCachePacket(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("m") UUID targetId
) implements NetworkPacket {
}
