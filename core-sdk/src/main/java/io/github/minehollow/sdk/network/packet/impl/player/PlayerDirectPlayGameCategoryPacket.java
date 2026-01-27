package io.github.minehollow.sdk.network.packet.impl.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;
import io.github.minehollow.sdk.server.type.ServerCategoryType;

import java.util.UUID;

public record PlayerDirectPlayGameCategoryPacket(
    @JsonProperty("i") UUID playerId,
    @JsonProperty("g") ServerCategoryType categoryType
) implements NetworkPacket { }
