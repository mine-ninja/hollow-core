package net.warcane.lugin.core.network.packet.impl.player.permission;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.player.permissions.PlayerPermission;

import java.util.UUID;

public record PlayerLosePermissionPacket(
    @JsonProperty("pid") UUID playerId,
    @JsonProperty("p") String permission
) implements NetworkPacket {
}
