package net.warcane.lugin.core.player;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlayerCount(
  @JsonProperty("o") int online,
  @JsonProperty("m") int max
) { }
