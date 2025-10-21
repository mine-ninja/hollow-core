package net.warcane.lugin.core.network.packet.impl.player.discord;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record PlayerUnlinkDiscordPacket(
    @JsonProperty("i") UUID playerId
) implements NetworkPacket { }
