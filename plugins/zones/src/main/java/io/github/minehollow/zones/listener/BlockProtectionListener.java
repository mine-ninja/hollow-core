package io.github.minehollow.zones.listener;

import io.github.minehollow.zones.ZoneQuery;
import io.github.minehollow.zones.model.ZoneFlag;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;

import java.util.UUID;

/**
 * Protects blocks: break, place, physics, tick (growth/fire).
 */
@RequiredArgsConstructor
public class BlockProtectionListener implements Listener {

    private final ZoneQuery query;


    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (query.isDenied(e.getBlock().getLocation(), ZoneFlag.BLOCK_BREAK, e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (query.isDenied(e.getBlock().getLocation(), ZoneFlag.BLOCK_PLACE, e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent e) {
        if (query.isDenied(e.getBlock().getLocation(), ZoneFlag.BLOCK_PHYSICS, (UUID) null)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent e) {
        if (query.isDenied(e.getBlock().getLocation(), ZoneFlag.BLOCK_TICK, (UUID) null)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        if (query.isDenied(e.getBlock().getLocation(), ZoneFlag.BLOCK_TICK, (UUID) null)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) {
        if (query.isDenied(e.getBlock().getLocation(), ZoneFlag.BLOCK_TICK, (UUID) null)) {
            e.setCancelled(true);
        }
    }
}

