package io.github.minehollow.sdk.network.packet.impl.player.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.UUID;

public record PlayerReceivePermissionPacket(
    @JsonProperty("pid") UUID playerId,
    @JsonProperty("p") String permission
) implements NetworkPacket {
}
