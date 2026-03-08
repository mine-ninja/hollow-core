package io.github.minehollow.essentials.tablist;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages above-head nametags and tab-list ordering using PacketEvents
 * scoreboard team packets, implementing the Minikloon approach.
 */
public class NameTagManager {

    private static final String TEAM_NAME_PREFIX = "hc_";
    private static final Component EMPTY = Component.empty();

    private final Map<UUID, PlayerTag> tags = new ConcurrentHashMap<>();
    private final @Nullable LuckPermsHook luckPermsHook;

    // Cached — avoid repeated API lookups on hot path
    private final PlayerManager packetPlayerManager;

    public NameTagManager(@Nullable LuckPermsHook luckPermsHook) {
        this.luckPermsHook = luckPermsHook;
        this.packetPlayerManager = PacketEvents.getAPI().getPlayerManager();
    }

    private static final class PlayerTag {
        final String teamName;
        final String tablistUsername;
        String dirtyKey;
        Component teamPrefix;
        Component teamSuffix;

        PlayerTag(String teamName, String tablistUsername, String dirtyKey,
                  Component teamPrefix, Component teamSuffix) {
            this.teamName = teamName;
            this.tablistUsername = tablistUsername;
            this.dirtyKey = dirtyKey;
            this.teamPrefix = teamPrefix;
            this.teamSuffix = teamSuffix;
        }
    }

    /**
     * Phase 1 (called from onPreJoin): registers the player's team and broadcasts
     * the team CREATE to all currently online players, so the nametag is visible
     * immediately when the ADD_PLAYER packet arrives. Does NOT send existing teams
     * to the joining player (they may not be ready to receive packets yet).
     */
    public void registerTeam(@NotNull Player player, @NotNull String tablistUsername,
                             @NotNull Component fullPrefix, @NotNull Component fullSuffix,
                             @NotNull String dirtyKey) {
        int weight = getWeight(player);
        String teamName = buildTeamName(weight, player.getName());
        tags.put(player.getUniqueId(), new PlayerTag(teamName, tablistUsername, dirtyKey, fullPrefix, fullSuffix));

        // Broadcast this player's team to all currently online players
        WrapperPlayServerTeams createPacket = buildCreatePacket(teamName, tablistUsername, fullPrefix, fullSuffix);
        for (Player online : Bukkit.getOnlinePlayers()) {
            packetPlayerManager.sendPacket(online, createPacket);
        }
    }

    /**
     * Phase 2 (called from delayed onJoin): sends all existing teams to the
     * joining player so they can see everyone else's nametags.
     */
    public void sendExistingTeams(@NotNull Player player) {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            PlayerTag existing = tags.get(online.getUniqueId());
            if (existing == null) continue;
            packetPlayerManager.sendPacket(player,
                buildCreatePacket(existing.teamName, existing.tablistUsername, existing.teamPrefix, existing.teamSuffix));
        }
    }

    /**
     * Updates a player's nametag. Accepts a pre-built dirtyKey to avoid
     * string concatenation on every tick.
     * <p>
     * If the player's weight (rank) changed, the team is destroyed and re-created
     * with a new team name so tab-list sorting is updated.
     */
    public void update(@NotNull Player player,
                       @NotNull Component fullPrefix, @NotNull Component fullSuffix,
                       @NotNull String dirtyKey) {
        UUID uuid = player.getUniqueId();
        PlayerTag tag = tags.get(uuid);
        if (tag == null) return;

        // Dirty check — skip entirely if nothing changed
        if (dirtyKey.equals(tag.dirtyKey)) return;

        // Check if weight changed — if so, team name must be rebuilt for correct sort order
        int currentWeight = getWeight(player);
        String expectedTeamName = buildTeamName(currentWeight, player.getName());

        if (!expectedTeamName.equals(tag.teamName)) {
            // Weight changed (rank promotion/demotion) — remove old team, create new one
            WrapperPlayServerTeams removePacket = buildRemovePacket(tag.teamName);
            for (Player online : Bukkit.getOnlinePlayers()) {
                packetPlayerManager.sendPacket(online, removePacket);
            }

            // Replace tag with new team name
            PlayerTag newTag = new PlayerTag(expectedTeamName, tag.tablistUsername, dirtyKey, fullPrefix, fullSuffix);
            tags.put(uuid, newTag);

            WrapperPlayServerTeams createPacket = buildCreatePacket(expectedTeamName, tag.tablistUsername, fullPrefix, fullSuffix);
            for (Player online : Bukkit.getOnlinePlayers()) {
                packetPlayerManager.sendPacket(online, createPacket);
            }
            return;
        }

        // Weight unchanged — just update prefix/suffix content
        WrapperPlayServerTeams updatePacket = buildUpdatePacket(tag.teamName, fullPrefix, fullSuffix);
        for (Player online : Bukkit.getOnlinePlayers()) {
            packetPlayerManager.sendPacket(online, updatePacket);
        }

        tag.dirtyKey = dirtyKey;
        tag.teamPrefix = fullPrefix;
        tag.teamSuffix = fullSuffix;
    }

    public void onQuit(@NotNull Player player) {
        PlayerTag tag = tags.remove(player.getUniqueId());
        if (tag == null) return;

        WrapperPlayServerTeams removePacket = buildRemovePacket(tag.teamName);
        for (Player online : Bukkit.getOnlinePlayers()) {
            packetPlayerManager.sendPacket(online, removePacket);
        }
    }

    // ── Packet builders ──────────────────────────────────────

    private WrapperPlayServerTeams buildCreatePacket(String teamName, String tablistUsername,
                                                      Component prefix, Component suffix) {
        ScoreBoardTeamInfo info = new ScoreBoardTeamInfo(
            EMPTY, prefix, suffix,
            NameTagVisibility.ALWAYS, CollisionRule.ALWAYS,
            NamedTextColor.WHITE, OptionData.NONE
        );
        return new WrapperPlayServerTeams(teamName, TeamMode.CREATE, info, tablistUsername);
    }

    private WrapperPlayServerTeams buildUpdatePacket(String teamName, Component prefix, Component suffix) {
        ScoreBoardTeamInfo info = new ScoreBoardTeamInfo(
            EMPTY, prefix, suffix,
            NameTagVisibility.ALWAYS, CollisionRule.ALWAYS,
            NamedTextColor.WHITE, OptionData.NONE
        );
        return new WrapperPlayServerTeams(teamName, TeamMode.UPDATE, info);
    }

    private WrapperPlayServerTeams buildRemovePacket(String teamName) {
        return new WrapperPlayServerTeams(teamName, TeamMode.REMOVE, (ScoreBoardTeamInfo) null);
    }

    // ── Helpers ──────────────────────────────────────────────

    private String buildTeamName(int weight, String playerName) {
        int invWeight = 999 - Math.clamp(weight, 0, 999);
        // Avoid String.format — manual zero-padding is faster
        String base = TEAM_NAME_PREFIX
            + (char) ('0' + invWeight / 100)
            + (char) ('0' + (invWeight / 10) % 10)
            + (char) ('0' + invWeight % 10)
            + "_";
        int remaining = 16 - base.length(); // 16 - 8 = 8
        if (playerName.length() > remaining) {
            return base + playerName.substring(0, remaining);
        }
        return base + playerName;
    }

    private int getWeight(Player player) {
        if (luckPermsHook != null) {
            int w = luckPermsHook.getWeight(player);
            if (w >= 0) return w;
        }
        return 0;
    }
}

