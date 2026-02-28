package io.github.minehollow.leaderboard.model;

import io.github.minehollow.sdk.stats.StatPeriod;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Persisted leaderboard configuration.
 *
 * @param id          unique leaderboard identifier
 * @param statKey     the stat key to rank by (e.g. "kills")
 * @param period      the time period for ranking
 * @param displayName display name shown in header (MiniMessage)
 * @param maxEntries  how many top entries to show
 * @param hologramLoc if non-null, a hologram is rendered at this location
 * @param headerLines lines rendered above the leaderboard entries (MiniMessage)
 * @param entryFormat format for each entry — placeholders: {position}, {player}, {value}
 * @param footerLines lines rendered below the leaderboard entries (MiniMessage)
 * @param emptyEntry  format for empty/unfilled slots
 * @param icon        material used as icon in the leaderboard menu
 */
public record LeaderboardConfig(
    @NotNull String id,
    @NotNull String statKey,
    @NotNull StatPeriod period,
    @NotNull String displayName,
    int maxEntries,
    @Nullable Location hologramLoc,
    @NotNull List<String> headerLines,
    @NotNull String entryFormat,
    @NotNull List<String> footerLines,
    @NotNull String emptyEntry,
    @NotNull Material icon
) {}
