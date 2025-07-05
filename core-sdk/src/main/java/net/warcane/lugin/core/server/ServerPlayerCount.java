package net.warcane.lugin.core.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

public record ServerPlayerCount(
  @JsonProperty("o") int online,
  @JsonProperty("m") int max
) {

    @NotNull
    public String toFormattedString() {
        return String.format("%d/%d", online, max);
    }
}
