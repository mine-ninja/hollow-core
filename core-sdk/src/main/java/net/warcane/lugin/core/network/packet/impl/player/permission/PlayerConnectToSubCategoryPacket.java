package net.warcane.lugin.core.network.packet.impl.player.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.server.type.ServerSubCategoryType;

import java.util.UUID;

public record PlayerConnectToSubCategoryPacket(
    @JsonProperty("i") UUID playerId,
    @JsonProperty("s") ServerSubCategoryType subCategoryType
) implements NetworkPacket { }
