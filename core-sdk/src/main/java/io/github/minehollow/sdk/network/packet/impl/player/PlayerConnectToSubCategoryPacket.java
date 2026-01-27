package io.github.minehollow.sdk.network.packet.impl.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;
import io.github.minehollow.sdk.server.type.ServerSubCategoryType;

import java.util.UUID;

public record PlayerConnectToSubCategoryPacket(
    @JsonProperty("i") UUID playerId,
    @JsonProperty("s") ServerSubCategoryType subCategoryType
) implements NetworkPacket { }
