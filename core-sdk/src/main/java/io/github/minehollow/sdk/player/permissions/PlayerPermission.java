package io.github.minehollow.sdk.player.permissions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerPermission(
    @JsonProperty("p") String permission,
    @JsonProperty("ps") Instant permissionStart,
    @JsonProperty("pe") Instant permissionEnd,
    @JsonProperty(value = "m", defaultValue = "{}") Map<String, Object> metadata
) {

    public static PlayerPermission createNewPermissions(String permission, Instant end) {
        return new PlayerPermission(permission, Instant.now(), end, new HashMap<>());
    }

    public static PlayerPermission createNewPermanentPermissions(String permission) {
        return new PlayerPermission(permission, Instant.now(), Instant.now(), Map.of("permanent", true));
    }

    @JsonIgnore
    public boolean isExpired() {
        return !isPermanent() && permissionEnd.isBefore(Instant.now());
    }

    @JsonIgnore
    public boolean isPermanent() {
        return (boolean) metadata.getOrDefault("permanent", false);
    }
}
