package io.github.minehollow.essentials.tablist;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate.*;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Intercepts outgoing {@code PLAYER_INFO_UPDATE} packets to implement the
 * Minikloon tab-list trick.
 * <p>
 * Each player's real username is replaced with a unique invisible string
 * made of {@code §} formatting codes. The team prefix/suffix provide the
 * full RGB display.
 */
public class TabListPacketListener extends PacketListenerAbstract {

    private static final char[] CODES = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'
    };

    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private static final EnumSet<Action> UPDATE_DISPLAY_NAME_SET =
        EnumSet.of(Action.UPDATE_DISPLAY_NAME);

    /** playerUUID → their unique invisible tablist username */
    private final Map<UUID, String> tablistUsernames = new ConcurrentHashMap<>();

    /** playerUUID → full display component (prefix + colored name + suffix) */
    private final Map<UUID, Component> displayNames = new ConcurrentHashMap<>();

    // Cached — avoid repeated API lookups
    private PlayerManager packetPlayerManager;

    private PlayerManager pm() {
        PlayerManager pm = packetPlayerManager;
        if (pm == null) {
            pm = PacketEvents.getAPI().getPlayerManager();
            packetPlayerManager = pm;
        }
        return pm;
    }

    // ── Tablist username generation ──────────────────────────

    private static @NotNull String generateTablistUsername() {
        int counter = COUNTER.getAndIncrement();
        StringBuilder name = new StringBuilder(12);
        while (counter > 0) {
            int remainder = counter % CODES.length;
            name.append('§').append(CODES[remainder]);
            counter /= CODES.length;
        }
        return name.toString();
    }

    // ── Public API ───────────────────────────────────────────

    public @NotNull String register(@NotNull UUID playerId, @NotNull Component displayName) {
        String tabUsername = tablistUsernames.computeIfAbsent(playerId, k -> generateTablistUsername());
        displayNames.put(playerId, displayName);
        return tabUsername;
    }

    public void updateDisplayName(@NotNull UUID playerId, @NotNull Component displayName) {
        displayNames.put(playerId, displayName);
    }

    public String getTablistUsername(@NotNull UUID playerId) {
        return tablistUsernames.get(playerId);
    }

    public void unregister(@NotNull UUID playerId) {
        tablistUsernames.remove(playerId);
        displayNames.remove(playerId);
    }

    // ── Batch display name sending ───────────────────────────

    /**
     * Sends a SINGLE {@code UPDATE_DISPLAY_NAME} packet containing ALL registered
     * players to every online player. This is O(viewers) instead of O(viewers × entries).
     */
    public void sendBatchDisplayNames(@NotNull Collection<? extends Player> viewers) {
        if (displayNames.isEmpty()) return;

        // Build ONE list of PlayerInfo entries for all registered players
        List<PlayerInfo> entries = new ArrayList<>(displayNames.size());
        for (var entry : displayNames.entrySet()) {
            entries.add(new PlayerInfo(
                new UserProfile(entry.getKey(), "", null),
                true, 0, GameMode.SURVIVAL, entry.getValue(), null
            ));
        }

        WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(
            UPDATE_DISPLAY_NAME_SET, entries
        );

        PlayerManager pm = pm();
        for (Player viewer : viewers) {
            pm.sendPacket(viewer, packet);
        }
    }

    /**
     * Sends all display names to a single viewer (used on join).
     */
    public void sendAllDisplayNamesTo(@NotNull Player viewer) {
        if (displayNames.isEmpty()) return;

        List<PlayerInfo> entries = new ArrayList<>(displayNames.size());
        for (var entry : displayNames.entrySet()) {
            entries.add(new PlayerInfo(
                new UserProfile(entry.getKey(), "", null),
                true, 0, GameMode.SURVIVAL, entry.getValue(), null
            ));
        }

        WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(
            UPDATE_DISPLAY_NAME_SET, entries
        );
        pm().sendPacket(viewer, packet);
    }

    /**
     * Sends one player's display name to all online players (used on join).
     */
    public void sendDisplayNameToAll(@NotNull UUID playerId) {
        Component displayName = displayNames.get(playerId);
        if (displayName == null) return;

        PlayerInfo info = new PlayerInfo(
            new UserProfile(playerId, "", null),
            true, 0, GameMode.SURVIVAL, displayName, null
        );

        WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(
            UPDATE_DISPLAY_NAME_SET, info
        );

        PlayerManager pm = pm();
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            pm.sendPacket(online, packet);
        }
    }

    // ── Packet interception ──────────────────────────────────

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.PLAYER_INFO_UPDATE) return;

        WrapperPlayServerPlayerInfoUpdate packet = new WrapperPlayServerPlayerInfoUpdate(event);
        var actions = packet.getActions();

        boolean hasAddPlayer = actions.contains(Action.ADD_PLAYER);
        boolean hasDisplayName = actions.contains(Action.UPDATE_DISPLAY_NAME);
        if (!hasAddPlayer && !hasDisplayName) return;

        boolean modified = false;
        List<PlayerInfo> entries = packet.getEntries();

        for (PlayerInfo entry : entries) {
            UserProfile profile = entry.getGameProfile();
            if (profile == null) continue;

            UUID uuid = profile.getUUID();
            String tabUsername = tablistUsernames.get(uuid);
            if (tabUsername == null) continue;

            Component displayName = displayNames.get(uuid);
            if (displayName == null) continue;

            if (hasAddPlayer) {
                entry.setGameProfile(new UserProfile(
                    uuid, tabUsername, profile.getTextureProperties()
                ));
            }

            entry.setDisplayName(displayName);
            modified = true;
        }

        if (modified) {
            event.markForReEncode(true);
        }
    }
}

