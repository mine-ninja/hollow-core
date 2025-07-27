package net.warcane.lugin.core.player.teleport;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.location.RemoteServerLocation;

import java.util.UUID;

public record PlayerJoinData(
  @JsonProperty("pid") UUID uniqueId,
  @JsonProperty("remote") RemoteServerLocation remoteServerLocation
) {

    public static PlayerJoinData create(UUID uniqueId, RemoteServerLocation remoteServerLocation) {
        return new PlayerJoinData(uniqueId, remoteServerLocation);
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public RemoteServerLocation getRemoteServerLocation() {
        return remoteServerLocation;
    }
}
