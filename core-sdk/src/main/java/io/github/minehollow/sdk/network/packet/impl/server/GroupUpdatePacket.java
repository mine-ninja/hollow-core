package io.github.minehollow.sdk.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.group.PlayerGroup;

public record GroupUpdatePacket(
  @JsonProperty("groupToUpdate") PlayerGroup playerGroup
)  {
}
