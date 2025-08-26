package net.warcane.lugin.core.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

public record ServerPlayers(
  @JsonProperty("o") Map<UUID, String> players,
  @JsonProperty("m") int max
) {
    @JsonIgnore
    public int online() {
        return players.size();
    }

    @JsonIgnore
    public boolean isFull() {
        return players.size() >= max;
    }

    @NotNull
    @JsonIgnore
    public String toFormattedString() {
        return String.format("%d/%d", players.size(), max);
    }
}
