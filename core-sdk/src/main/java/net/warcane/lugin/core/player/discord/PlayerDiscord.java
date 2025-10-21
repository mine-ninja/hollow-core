package net.warcane.lugin.core.player.discord;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.UUID;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlayerDiscord(
    @JsonProperty("pi") UUID playerId,
    @JsonProperty("di") String discordId,
    @JsonProperty("du") String discordUsername
) implements Serializable {

    public static PlayerDiscord createDefaultSettings(@NotNull UUID playerId) {
        return new PlayerDiscord(playerId, null, null);
    }

    public PlayerDiscord playerLinkDiscord(String discordId, String discordUsername) {
        return new PlayerDiscord(playerId, discordId, discordUsername);
    }

    public PlayerDiscord playerUnlinkDiscord() {
        return new PlayerDiscord(playerId, null, null);
    }

    @JsonIgnore
    public boolean isLinked() {
        return discordId != null && !discordId.isEmpty();
    }
}
