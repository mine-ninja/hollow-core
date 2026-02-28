package io.github.minehollow.leaderboard.hologram;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import io.github.minehollow.leaderboard.model.LeaderboardConfig;
import io.github.minehollow.sdk.stats.StatEntry;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Renders a leaderboard as a packet-based TextDisplay hologram.
 * Updated periodically with fresh data from the stat service.
 */
public class LeaderboardHologram {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final LeaderboardConfig config;
    private WrapperEntity entity;
    private final Set<UUID> viewers = new CopyOnWriteArraySet<>();
    private boolean spawned = false;

    public LeaderboardHologram(@NotNull LeaderboardConfig config) {
        this.config = config;
    }

    /**
     * Spawns the hologram entity at the configured location.
     */
    public void spawn() {
        if (spawned || config.hologramLoc() == null) return;

        entity = new WrapperEntity(EntityTypes.TEXT_DISPLAY);
        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setText(Component.text("Loading..."));
        meta.setBillboardConstraints(TextDisplayMeta.BillboardConstraints.CENTER);
        meta.setShadow(true);
        meta.setBackgroundColor(0x40000000);

        entity.spawn(SpigotConversionUtil.fromBukkitLocation(config.hologramLoc()));
        spawned = true;
    }

    /**
     * Despawns the hologram.
     */
    public void despawn() {
        if (!spawned || entity == null) return;
        spawned = false;
        viewers.clear();
        entity.despawn();
        entity.remove();
        entity = null;
    }

    /**
     * Updates the hologram text with the provided leaderboard entries.
     */
    public void update(@NotNull List<StatEntry> entries) {
        if (!spawned || entity == null) return;

        List<String> lines = new ArrayList<>();

        // Header
        lines.addAll(config.headerLines());

        // Entries
        for (int i = 0; i < config.maxEntries(); i++) {
            if (i < entries.size()) {
                StatEntry entry = entries.get(i);
                String playerName = resolvePlayerName(entry.playerId());
                String line = config.entryFormat()
                    .replace("{position}", String.valueOf(i + 1))
                    .replace("{player}", playerName)
                    .replace("{value}", formatValue(entry.value()));
                lines.add(line);
            } else {
                String line = config.emptyEntry()
                    .replace("{position}", String.valueOf(i + 1));
                lines.add(line);
            }
        }

        // Footer
        lines.addAll(config.footerLines());

        // Render
        String combined = String.join("\n", lines);
        Component text = MINI.deserialize(combined);
        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setText(text);
        entity.refresh();
    }

    public void addViewer(@NotNull Player player) {
        if (!spawned || entity == null) return;
        if (viewers.add(player.getUniqueId())) {
            entity.addViewer(player.getUniqueId());
        }
    }

    public void removeViewer(@NotNull Player player) {
        if (viewers.remove(player.getUniqueId()) && entity != null) {
            entity.removeViewer(player.getUniqueId());
        }
    }

    public boolean isViewer(@NotNull UUID uuid) {
        return viewers.contains(uuid);
    }

    public boolean isSpawned() {
        return spawned;
    }

    public @Nullable Location getLocation() {
        return config.hologramLoc();
    }

    public @NotNull String getId() {
        return config.id();
    }

    // ── Helpers ──────────────────────────────────────────────

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

