package net.warcane.lugin.core.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.group.PlayerGroup;

public record GroupUpdatePacket(
  @JsonProperty("groupToUpdate") PlayerGroup playerGroup
)  {
}
