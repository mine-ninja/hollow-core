package io.github.minehollow.sdk.network.packet.impl.player.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.UUID;

public record PlayerUnlinkDiscordPacket(
    @JsonProperty("i") UUID playerId
) implements NetworkPacket { }
