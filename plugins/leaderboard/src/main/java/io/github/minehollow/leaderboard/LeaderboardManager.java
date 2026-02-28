    package io.github.minehollow.leaderboard;

import io.github.minehollow.leaderboard.hologram.LeaderboardHologram;
import io.github.minehollow.leaderboard.model.LeaderboardConfig;
import io.github.minehollow.leaderboard.storage.LeaderboardStorage;
import io.github.minehollow.sdk.stats.StatEntry;
import io.github.minehollow.sdk.stats.StatPeriod;
import io.github.minehollow.sdk.stats.StatService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for leaderboards. Manages configs, holograms, and data refresh.
 */
public class LeaderboardManager {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final double HOLOGRAM_VIEW_DISTANCE_SQ = 48.0 * 48.0;

    private final StatService statService;
    private final LeaderboardStorage storage;

    private final Map<String, LeaderboardConfig> configs = new ConcurrentHashMap<>();
    private final Map<String, LeaderboardHologram> holograms = new ConcurrentHashMap<>();

    /** Cached leaderboard data — refreshed periodically by the update task. */
    private final Map<String, List<StatEntry>> cachedEntries = new ConcurrentHashMap<>();

    public LeaderboardManager(@NotNull StatService statService, @NotNull LeaderboardStorage storage) {
        this.statService = statService;
        this.storage = storage;
    }

    // ── Lifecycle ────────────────────────────────────────────

    /**
     * Loads all leaderboards from storage and spawns holograms.
     */
    public void loadAll() {
        configs.putAll(storage.loadAll());
        for (LeaderboardConfig config : configs.values()) {
            if (config.hologramLoc() != null) {
                LeaderboardHologram holo = new LeaderboardHologram(config);
                holo.spawn();
                holograms.put(config.id(), holo);
            }
        }
    }

    /**
     * Saves all configs and despawns holograms.
     */
    public void unloadAll() {
        storage.saveAll(new LinkedHashMap<>(configs));
        for (LeaderboardHologram holo : holograms.values()) {
            holo.despawn();
        }
        holograms.clear();
        configs.clear();
        cachedEntries.clear();
    }

    /**
     * Saves all configs to disk.
     */
    public void save() {
        storage.saveAll(new LinkedHashMap<>(configs));
    }

    // ── CRUD ─────────────────────────────────────────────────

    /**
     * Creates a new leaderboard with defaults.
     */
    public @NotNull LeaderboardConfig create(@NotNull String id, @NotNull String statKey,
                                              @NotNull StatPeriod period, @Nullable Location hologramLoc) {
        return create(id, statKey, period, hologramLoc, Material.DIAMOND_SWORD);
    }

    public @NotNull LeaderboardConfig create(@NotNull String id, @NotNull String statKey,
                                              @NotNull StatPeriod period, @Nullable Location hologramLoc,
                                              @NotNull Material icon) {
        if (configs.containsKey(id)) {
            throw new IllegalArgumentException("Leaderboard '" + id + "' already exists.");
        }

        LeaderboardConfig config = new LeaderboardConfig(
            id, statKey, period,
            "<gold><bold>" + id + "</bold></gold>",
            10,
            hologramLoc,
            List.of("<gray>━━━━━━━━━━━━━━━━━━━━", "<gold><bold>" + id + "</bold></gold>", "<gray>━━━━━━━━━━━━━━━━━━━━"),
            "<yellow>{position}. <white>{player} <gray>- <aqua>{value}",
            List.of("<gray>━━━━━━━━━━━━━━━━━━━━"),
            "<dark_gray>{position}. ---",
            icon
        );

        configs.put(id, config);

        if (hologramLoc != null) {
            LeaderboardHologram holo = new LeaderboardHologram(config);
            holo.spawn();
            holograms.put(id, holo);
            // Show to nearby players
            for (Player p : Bukkit.getOnlinePlayers()) {
                updateVisibility(p, holo);
            }
        }

        save();
        return config;
    }

    /**
     * Removes a leaderboard.
     */
    public boolean remove(@NotNull String id) {
        configs.remove(id);
        cachedEntries.remove(id);
        LeaderboardHologram holo = holograms.remove(id);
        if (holo != null) holo.despawn();
        save();
        return holo != null || configs.containsKey(id);
    }

    /**
     * Moves a leaderboard hologram to a new location.
     */
    public void setHologramLocation(@NotNull String id, @NotNull Location location) {
        LeaderboardConfig old = configs.get(id);
        if (old == null) throw new IllegalArgumentException("Leaderboard '" + id + "' not found.");

        LeaderboardConfig updated = new LeaderboardConfig(
            old.id(), old.statKey(), old.period(), old.displayName(), old.maxEntries(),
            location, old.headerLines(), old.entryFormat(), old.footerLines(), old.emptyEntry(),
            old.icon()
        );
        configs.put(id, updated);

        // Respawn hologram at new location
        LeaderboardHologram oldHolo = holograms.remove(id);
        if (oldHolo != null) oldHolo.despawn();

        LeaderboardHologram holo = new LeaderboardHologram(updated);
        holo.spawn();
        holograms.put(id, holo);

        for (Player p : Bukkit.getOnlinePlayers()) {
            updateVisibility(p, holo);
        }

        save();
    }

    public @Nullable LeaderboardConfig get(@NotNull String id) {
        return configs.get(id);
    }

    public @NotNull Collection<LeaderboardConfig> getAll() {
        return configs.values();
    }

    // ── Data Refresh ─────────────────────────────────────────

    /**
     * Refreshes leaderboard data from MongoDB and updates all holograms.
     * Should be called periodically from an async task.
     */
    public void refreshAll() {
        for (LeaderboardConfig config : configs.values()) {
            List<StatEntry> entries = statService.leaderboard(
                config.statKey(), config.period(), config.maxEntries());
            cachedEntries.put(config.id(), entries);

            LeaderboardHologram holo = holograms.get(config.id());
            if (holo != null && holo.isSpawned()) {
                holo.update(entries);
            }
        }
    }

    /**
     * Returns cached leaderboard entries for a given leaderboard.
     */
    public @NotNull List<StatEntry> getCachedEntries(@NotNull String id) {
        return cachedEntries.getOrDefault(id, List.of());
    }

    // ── Player Position ──────────────────────────────────────

    /**
     * Gets a player's rank in a leaderboard. Blocking — call from async.
     */
    public long getPlayerRank(@NotNull UUID playerId, @NotNull String leaderboardId) {
        LeaderboardConfig config = configs.get(leaderboardId);
        if (config == null) return -1;
        return statService.getRank(playerId, config.statKey(), config.period());
    }

    /**
     * Gets a player's stat value for a leaderboard.
     */
    public long getPlayerValue(@NotNull UUID playerId, @NotNull String leaderboardId) {
        LeaderboardConfig config = configs.get(leaderboardId);
        if (config == null) return 0;
        return statService.get(playerId, config.statKey(), config.period());
    }

    // ── Hologram Visibility ──────────────────────────────────

    /**
     * Updates hologram visibility for a player based on distance.
     */
    public void updateVisibility(@NotNull Player player) {
        for (LeaderboardHologram holo : holograms.values()) {
            updateVisibility(player, holo);
        }
    }

    private void updateVisibility(@NotNull Player player, @NotNull LeaderboardHologram holo) {
        if (!holo.isSpawned()) return;
        Location holoLoc = holo.getLocation();
        if (holoLoc == null || holoLoc.getWorld() == null) return;

        Location playerLoc = player.getLocation();
        if (!holoLoc.getWorld().equals(playerLoc.getWorld())) {
            if (holo.isViewer(player.getUniqueId())) {
                holo.removeViewer(player);
            }
            return;
        }

        double distSq = holoLoc.distanceSquared(playerLoc);
        if (distSq <= HOLOGRAM_VIEW_DISTANCE_SQ) {
            if (!holo.isViewer(player.getUniqueId())) {
                holo.addViewer(player);
            }
        } else {
            if (holo.isViewer(player.getUniqueId())) {
                holo.removeViewer(player);
            }
        }
    }

    /**
     * Removes a player from all holograms (e.g. on quit).
     */
    public void removeViewer(@NotNull Player player) {
        for (LeaderboardHologram holo : holograms.values()) {
            holo.removeViewer(player);
        }
    }

    // ── Chat Rendering ───────────────────────────────────────

    /**
     * Builds chat messages for a leaderboard (used by the command).
     */
    public @NotNull List<Component> buildChatLines(@NotNull String leaderboardId, @Nullable Player viewer) {
        LeaderboardConfig config = configs.get(leaderboardId);
        if (config == null) return List.of(Component.text("§cLeaderboard not found."));

        List<StatEntry> entries = cachedEntries.getOrDefault(leaderboardId, List.of());
        List<Component> lines = new ArrayList<>();

        // Header
        for (String h : config.headerLines()) {
            lines.add(MINI.deserialize(h));
        }

        // Entries
        for (int i = 0; i < config.maxEntries(); i++) {
            if (i < entries.size()) {
                StatEntry entry = entries.get(i);
                String name = resolvePlayerName(entry.playerId());
                String line = config.entryFormat()
                    .replace("{position}", String.valueOf(i + 1))
                    .replace("{player}", name)
                    .replace("{value}", formatValue(entry.value()));
                lines.add(MINI.deserialize(line));
            } else {
                String line = config.emptyEntry()
                    .replace("{position}", String.valueOf(i + 1));
                lines.add(MINI.deserialize(line));
            }
        }

        // Footer
        for (String f : config.footerLines()) {
            lines.add(MINI.deserialize(f));
        }

        // Player position
        if (viewer != null) {
            long rank = getPlayerRank(viewer.getUniqueId(), leaderboardId);
            long value = getPlayerValue(viewer.getUniqueId(), leaderboardId);
            String pos = rank > 0
                ? "<gray>Sua posição: <yellow>#" + rank + " <gray>(<aqua>" + formatValue(value) + "<gray>)"
                : "<gray>Você ainda não possui dados neste ranking.";
            lines.add(Component.empty());
            lines.add(MINI.deserialize(pos));
        }

        return lines;
    }

    private @NotNull String resolvePlayerName(@NotNull UUID playerId) {
        Player online = Bukkit.getPlayer(playerId);
        if (online != null) return online.getName();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
        String name = offline.getName();
        return name != null ? name : playerId.toString().substring(0, 8);
    }

    private @NotNull String formatValue(long value) {
        if (value >= 1_000_000) return String.format("%.1fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }
}

