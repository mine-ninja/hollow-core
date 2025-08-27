package net.warcane.lugin.core.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerPlayers(
  @JsonProperty("o") int online,
  @JsonProperty("m") int max
) {
    @JsonIgnore
    public int online() {
        return online;
    }

    @JsonIgnore
    public boolean isFull() {
        return online >= max;
    }

    @NotNull
    @JsonIgnore
    public String toFormattedString() {
        return String.format("%d/%d", online, max);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return online <= 0;
    }
}
