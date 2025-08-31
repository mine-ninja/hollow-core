package net.warcane.lugin.core.network.packet.impl.player;


import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record SendMessageToPlayerPacket(
  @JsonProperty("i") UUID playerId,
  @JsonProperty("m") String message,
  @JsonProperty("k") String key
) implements NetworkPacket {
    public SendMessageToPlayerPacket(UUID playerId, String message) {
        this(playerId, message, "unknown");
    }
}
