package net.warcane.lugin.core.minecraft.npc.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.npc.Npc;
import net.warcane.lugin.core.minecraft.npc.NpcManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class NpcRenderListener implements Listener {

    private final NpcManager npcManager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        updatePlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(@NotNull PlayerJoinEvent event) {
        this.removeAllNpcsForPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(@NotNull PlayerChangedWorldEvent event) {
        updatePlayer(event.getPlayer());
    }

    @EventHandler
    public void onWorldUnload(@NotNull WorldUnloadEvent event) {
        npcManager.getCachedNpcMap().forEachValue(npc -> {
            if (npc.getLocation().getWorld().equals(event.getWorld())) {
                npcManager.unloadNpc(npc);
            }
            return true;
        });
    }


    public void removeAllNpcsForPlayer(@NotNull Player player) {
        npcManager.getCachedNpcMap().forEachValue(npc -> {
            removeNpcForPlayer(player, npc);
            return true;
        });
    }

    public void removeNpcForPlayer(@NotNull Player player, @NotNull Npc npc) {
        npc.getVisibilityHandler().hideTo(player);
    }

    public void updatePlayer(@NotNull Player player) {
        npcManager.getCachedNpcMap().forEachValue(npc -> {
            updatePlayer(player, npc);
            return true;
        });
    }

    public void updatePlayer(@NotNull Player player, @NotNull Npc npc) {
        var playerLocation = player.getLocation();
        var npcLocation = npc.getLocation();
        var visibilityHandler = npc.getVisibilityHandler();

        if (!playerLocation.getWorld().equals(npcLocation.getWorld())) {
            visibilityHandler.hideTo(player);
        } else if (visibilityHandler.canView(player)) {
            visibilityHandler.showTo(player);
        } else {
            visibilityHandler.hideTo(player);
        }
    }
}
