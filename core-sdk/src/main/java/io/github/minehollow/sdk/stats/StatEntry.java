package io.github.minehollow.sdk.stats;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * A single leaderboard entry.
 *
 * @param playerId the player's UUID
 * @param key      the stat key
 * @param value    the stat value
 */
public record StatEntry(@NotNull UUID playerId, @NotNull String key, long value) {}

