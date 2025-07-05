package net.warcane.lugin.core.network.packet.impl.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record PlayerJoinResponsePacket(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("o") String originServerId,
  @JsonProperty("t") String targetId
) implements NetworkPacket { }