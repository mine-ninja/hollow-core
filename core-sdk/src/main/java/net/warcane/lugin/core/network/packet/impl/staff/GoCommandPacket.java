package net.warcane.lugin.core.network.packet.impl.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record GoCommandPacket(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("m") String targetName
) implements NetworkPacket {
}
