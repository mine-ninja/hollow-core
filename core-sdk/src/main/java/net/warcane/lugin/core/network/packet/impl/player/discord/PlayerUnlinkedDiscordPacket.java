package net.warcane.lugin.core.network.packet.impl.player.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record PlayerUnlinkedDiscordPacket(
    @JsonProperty("i") UUID playerId,
    @JsonProperty("e") boolean hasError,
    @JsonProperty("m") String message
) implements NetworkPacket { }
