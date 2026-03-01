package io.github.minehollow.zones.listener;

import io.github.minehollow.zones.ZoneQuery;
import io.github.minehollow.zones.model.ZoneFlag;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Protects player actions: item-drop, item-pickup, command-execute, interact.
 */
@RequiredArgsConstructor
public class PlayerProtectionListener implements Listener {

    private final ZoneQuery query;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (query.isDenied(e.getPlayer().getLocation(), ZoneFlag.ITEM_DROP, e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (query.isDenied(player.getLocation(), ZoneFlag.ITEM_PICKUP, player.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (query.isDenied(e.getPlayer().getLocation(), ZoneFlag.COMMAND_EXECUTE, e.getPlayer().getUniqueId())) {
            e.getPlayer().sendMessage("§cVocê não pode executar comandos nesta zona.");
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (query.isDenied(e.getClickedBlock().getLocation(), ZoneFlag.INTERACT, e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }
}

