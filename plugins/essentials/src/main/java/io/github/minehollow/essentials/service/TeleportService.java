package io.github.minehollow.essentials.service;

import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.event.tick.AsyncServerTickEvent;
import io.github.minehollow.minecraft.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles delayed teleportation with optional cancel-on-move.
 * <p>
 * Uses a single {@link AsyncServerTickEvent} listener to process all pending
 * teleports instead of spawning a task per teleport.
 */
public class TeleportService implements Listener {

    private final int delayTicks;
    private final boolean cancelOnMove;
    private final MessageConfig messages;

    /** All pending teleports keyed by player UUID. */
    private final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();

    public TeleportService(int delaySeconds, boolean cancelOnMove, @NotNull MessageConfig messages) {
        this.delayTicks = delaySeconds * 20;
        this.cancelOnMove = cancelOnMove;
        this.messages = messages;
    }

    // ── Tick handler (single async task) ─────────────────────

    @EventHandler
    public void onTick(@NotNull AsyncServerTickEvent event) {
        if (pending.isEmpty()) return;

        Iterator<Map.Entry<UUID, PendingTeleport>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingTeleport> entry = it.next();
            UUID playerId = entry.getKey();
            PendingTeleport tp = entry.getValue();

            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                it.remove();
                continue;
            }

            // Immediate teleport (delay = 0 or bypass) — already decremented to 0
            if (tp.remainingTicks <= 0) {
                it.remove();
                player.teleportAsync(tp.destination);
                continue;
            }

            // Cancel on move check
            if (cancelOnMove && hasMoved(player.getLocation(), tp.startLocation)) {
                it.remove();
                messages.send(player, "tp-cancelled-move");
                continue;
            }

            tp.remainingTicks--;
        }
    }

    // ── Public API ───────────────────────────────────────────

    /**
     * Teleports a player to a location, respecting the configured delay.
     */
    public void teleport(@NotNull Player player, @NotNull Location destination) {
        UUID id = player.getUniqueId();

        // Already has a pending teleport
        if (pending.containsKey(id)) return;

        int ticks = (delayTicks <= 0 || player.hasPermission("hollow.teleport.bypass")) ? 0 : delayTicks;

        pending.put(id, new PendingTeleport(
            destination.clone(),
            player.getLocation().clone(),
            ticks
        ));

        if (ticks > 0) {
            messages.send(player, "tp-delay", "delay", String.valueOf(ticks / 20));
        }
    }

    /**
     * Called from the move listener to cancel pending teleports on movement.
     */
    public void onPlayerMove(@NotNull Player player) {
        if (!cancelOnMove) return;

        PendingTeleport tp = pending.get(player.getUniqueId());
        if (tp == null) return;

        if (hasMoved(player.getLocation(), tp.startLocation)) {
            pending.remove(player.getUniqueId());
            messages.send(player, "tp-cancelled-move");
        }
    }

    /**
     * Cancels any pending teleport for a player (e.g. on quit).
     */
    public void cancel(@NotNull UUID playerId) {
        pending.remove(playerId);
    }

    // ── Internal ─────────────────────────────────────────────

    private boolean hasMoved(@NotNull Location current, @NotNull Location start) {
        return current.getBlockX() != start.getBlockX()
            || current.getBlockY() != start.getBlockY()
            || current.getBlockZ() != start.getBlockZ();
    }

    private static final class PendingTeleport {
        final Location destination;
        final Location startLocation;
        int remainingTicks;

        PendingTeleport(@NotNull Location destination, @NotNull Location startLocation, int remainingTicks) {
            this.destination = destination;
            this.startLocation = startLocation;
            this.remainingTicks = remainingTicks;
        }
    }
}
