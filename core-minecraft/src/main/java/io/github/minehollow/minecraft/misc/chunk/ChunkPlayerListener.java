package io.github.minehollow.minecraft.misc.chunk;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class ChunkPlayerListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event instanceof PlayerTeleportEvent) return;

        final Location from = event.getFrom();
        final Location to = event.getTo();

        final int fX = from.getBlockX() >> 4;
        final int fZ = from.getBlockZ() >> 4;
        final int tX = to.getBlockX() >> 4;
        final int tZ = to.getBlockZ() >> 4;

        if (fX == tX && fZ == tZ) return;

        ChunkPlayerCountCache.updatePresence(fX, fZ, -1);
        ChunkPlayerCountCache.updatePresence(tX, tZ, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        final int fX = event.getFrom().getBlockX() >> 4;
        final int fZ = event.getFrom().getBlockZ() >> 4;
        final int tX = event.getTo().getBlockX() >> 4;
        final int tZ = event.getTo().getBlockZ() >> 4;

        if (fX == tX && fZ == tZ) return;

        ChunkPlayerCountCache.updatePresence(fX, fZ, -1);
        ChunkPlayerCountCache.updatePresence(tX, tZ, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        final Location loc = event.getPlayer().getLocation();
        ChunkPlayerCountCache.updatePresence(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        final Location loc = event.getPlayer().getLocation();
        ChunkPlayerCountCache.updatePresence(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, -1);
    }
}