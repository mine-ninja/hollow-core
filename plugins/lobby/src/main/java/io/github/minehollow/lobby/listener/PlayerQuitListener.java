package io.github.minehollow.lobby.listener;

import io.github.minehollow.lobby.hologram.HologramManager;
import io.github.minehollow.lobby.npc.NPCManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerQuitListener implements Listener {
    private final HologramManager hologramManager;
    private final NPCManager npcManager;

    public PlayerQuitListener(HologramManager hologramManager, NPCManager npcManager) {
        this.hologramManager = hologramManager;
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent e) {
        var player = e.getPlayer();
        hologramManager.removeViewerFromAll(player);
        npcManager.removeViewerFromAll(player);

        e.quitMessage(null);
    }
}