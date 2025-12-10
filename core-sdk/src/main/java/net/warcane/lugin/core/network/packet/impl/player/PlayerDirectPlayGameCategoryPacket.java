package net.warcane.lugin.core.network.packet.impl.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.server.type.ServerCategoryType;

import java.util.UUID;

public record PlayerDirectPlayGameCategoryPacket(
    @JsonProperty("i") UUID playerId,
    @JsonProperty("g") ServerCategoryType categoryType
) implements NetworkPacket { }
