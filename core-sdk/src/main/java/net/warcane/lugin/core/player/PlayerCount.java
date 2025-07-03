package net.warcane.lugin.core.player;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

public record PlayerCount(
  @JsonProperty("o") int online,
  @JsonProperty("m") int max
) {

    @NotNull
    public String toFormattedString() {
        return String.format("%d/%d", online, max);
    }
}
