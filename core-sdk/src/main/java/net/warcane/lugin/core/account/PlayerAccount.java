package net.warcane.lugin.core.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.permission.PlayerGroup;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

public record PlayerAccount(
  @JsonProperty("i") UUID uniqueId,
  @JsonProperty("n") String playerName,
  @JsonProperty("g") PlayerGroup primaryGroup,
  @JsonProperty("c") Instant createdAt,
  @JsonProperty("l") Instant lastLogin
) implements Serializable {

    public boolean hasGroupPowers(@NotNull PlayerGroup group) {
        return primaryGroup.isGreaterOrEqualTo(group);
    }

    @NotNull
    public static PlayerAccount createDefaultAccount(@NotNull UUID uniqueId, @NotNull String playerName) {
        return new PlayerAccount(uniqueId, playerName, PlayerGroup.MEMBER, Instant.now(), Instant.now());
    }

    @NotNull
    public String getFormattedDisplayName() {
        return this.getFormattedTagInput(playerName);
    }

    public String getFormattedTagInput(@NotNull String input) {
        final var groupPrefix = primaryGroup.getPrefix();
        return groupPrefix + " §" + primaryGroup.getPrefixColorCode() + input;
    }
}
