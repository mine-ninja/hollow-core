package io.github.minehollow.sdk.network.packet.impl.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.UUID;

public record PlayerJoinResponsePacket(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("o") String originServerId,
  @JsonProperty("t") String targetId
) implements NetworkPacket { }