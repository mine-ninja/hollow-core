package net.warcane.lugin.core.network.packet.impl.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record PlayerUpdateAccountCachePacket(
    @JsonProperty("i") UUID playerId
) implements NetworkPacket { }
