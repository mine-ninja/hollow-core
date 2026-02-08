package io.github.minehollow.lobby.npc;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record NPCData(
  @NotNull String name,
  @NotNull Location location,
  @Nullable String skinTexture,
  @Nullable String skinSignature,
  @Nullable String hologramId,
  @Nullable String interaction
) {
}