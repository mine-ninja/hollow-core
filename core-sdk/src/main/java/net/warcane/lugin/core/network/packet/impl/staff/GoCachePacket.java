package net.warcane.lugin.core.network.packet.impl.staff;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record GoCachePacket(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("m") UUID targetId
) implements NetworkPacket {
}
