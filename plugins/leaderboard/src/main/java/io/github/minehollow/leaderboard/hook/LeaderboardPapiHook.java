package io.github.minehollow.leaderboard.hook;

import io.github.minehollow.leaderboard.LeaderboardManager;
import io.github.minehollow.leaderboard.model.LeaderboardConfig;
import io.github.minehollow.sdk.stats.StatEntry;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * PlaceholderAPI expansion for leaderboard data.
 *
 * Placeholders:
 *   %leaderboard_<id>_rank%           — player's rank in leaderboard
 *   %leaderboard_<id>_value%          — player's value in leaderboard
 *   %leaderboard_<id>_top_<n>_name%   — name of player at position n
 *   %leaderboard_<id>_top_<n>_value%  — value at position n
 */
public class LeaderboardPapiHook extends PlaceholderExpansion {

    private final LeaderboardManager manager;

    public LeaderboardPapiHook(@NotNull LeaderboardManager manager) {
        this.manager = manager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "leaderboard";
    }

    @Override
    public @NotNull String getAuthor() {
        return "sasuked";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer offlinePlayer, @NotNull String params) {
        // params format: <id>_rank, <id>_value, <id>_top_<n>_name, <id>_top_<n>_value
        String[] parts = params.split("_", 3);
        if (parts.length < 2) return null;

        String id = parts[0];
        LeaderboardConfig config = manager.get(id);
        if (config == null) return null;

        String action = parts[1];

        switch (action) {
            case "rank" -> {
                if (offlinePlayer == null) return "-";
                long rank = manager.getPlayerRank(offlinePlayer.getUniqueId(), id);
                return rank > 0 ? String.valueOf(rank) : "-";
            }
            case "value" -> {
                if (offlinePlayer == null) return "0";
                long value = manager.getPlayerValue(offlinePlayer.getUniqueId(), id);
                return String.valueOf(value);
            }
            case "top" -> {
                // <id>_top_<n>_name or <id>_top_<n>_value
                if (parts.length < 3) return null;
                String[] topParts = parts[2].split("_", 2);
                if (topParts.length < 2) return null;

                int position;
                try {
                    position = Integer.parseInt(topParts[0]) - 1; // convert 1-based to 0-based
                } catch (NumberFormatException e) {
                    return null;
                }

                List<StatEntry> entries = manager.getCachedEntries(id);
                if (position < 0 || position >= entries.size()) {
                    return topParts[1].equals("name") ? "---" : "0";
                }

                StatEntry entry = entries.get(position);
                return switch (topParts[1]) {
                    case "name" -> resolvePlayerName(entry.playerId());
                    case "value" -> String.valueOf(entry.value());
                    default -> null;
                };
            }
            default -> {
                return null;
            }
        }
    }

    private @NotNull String resolvePlayerName(@NotNull UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) return online.getName();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        String name = offline.getName();
        return name != null ? name : "???";
    }
}

