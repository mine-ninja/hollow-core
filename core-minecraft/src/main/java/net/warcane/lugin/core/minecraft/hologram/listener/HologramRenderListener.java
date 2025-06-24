package net.warcane.lugin.core.minecraft.hologram.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.hologram.Hologram;
import net.warcane.lugin.core.minecraft.hologram.HologramManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class HologramRenderListener implements Listener {

    private final HologramManager manager;


    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        updateAllHologramsForPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.removeAllHologramsForPlayer(event.getPlayer());
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        manager.getCachedHologramMap().forEach((uuid, hologram) -> {
            if (hologram.matchesWithWorld(event.getWorld())) {
                hologram.hideToAll();
                manager.removeHologram(uuid);
            }
        });
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        updateAllHologramsForPlayer(event.getPlayer());
    }

    public void removeAllHologramsForPlayer(@NotNull Player viewer) {
        manager.getCachedHologramMap().forEach((uuid, hologram) -> {
            hologram.hideTo(viewer);
        });
    }

    public void updateAllHologramsForPlayer(@NotNull Player viewer) {
        updateAllHologramsForPlayer(viewer, false);
    }

    public void updateAllHologramsForPlayer(@NotNull Player viewer, boolean fromTask) {
        manager.getCachedHologramMap().forEach((uuid, hologram) -> {
            updateHologramForPlayer(hologram, viewer, fromTask);
        });
    }

    public void updateHologramForPlayer(@NotNull Hologram hologram, @NotNull Player viewer) {
        updateHologramForPlayer(hologram, viewer, false);
    }

    public void updateHologramForPlayer(@NotNull Hologram hologram, @NotNull Player viewer, boolean fromTask) {
        if (!hologram.matchesWithWorld(viewer.getWorld())) {
            hologram.hideTo(viewer);
        } else if (hologram.canView(viewer)) {
            if (hologram.isShown(viewer)) {
                boolean canUpdate = fromTask && hologram.isAutoUpdate() && hologram.isExpired();

//                viewer.sendMessage(new String[]{
//                  "§cholo_id: " + hologram.getUniqueId(),
//                  "§cloc: " + hologram.getLocation().toString(),
//                  "§cupdate_interval: " + hologram.getUpdateInterval(),
//                  "§cis_auto_update: " + hologram.isAutoUpdate(),
//                  "§cfrom_task: " + fromTask,
//                  "§cis_expired: " + hologram.isExpired()
//                });
                if (canUpdate) {
//                    viewer.sendMessage("§aUpdating hologram: " + hologram.getUniqueId());
                    hologram.updateAllLines(viewer);
                    hologram.setUpdateInterval(System.currentTimeMillis() + hologram.getUpdateInterval());
                }
            } else {
                hologram.showTo(viewer);
            }
        } else {
            hologram.hideTo(viewer);
        }
    }
}
