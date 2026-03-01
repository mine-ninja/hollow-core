package io.github.minehollow.zones.listener;

import io.github.minehollow.zones.ZoneQuery;
import io.github.minehollow.zones.model.ZoneFlag;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

/**
 * Protects against entity spawns, mob spawns, and PvP.
 */
@RequiredArgsConstructor
public class EntityProtectionListener implements Listener {

    private final ZoneQuery query;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        var loc = e.getLocation();

        // mob-spawn check (hostile mobs only)
        if (e.getEntity() instanceof Monster) {
            if (query.isDenied(loc, ZoneFlag.MOB_SPAWN, (UUID) null)) {
                e.setCancelled(true);
                return;
            }
        }

        // general entity-spawn check
        if (query.isDenied(loc, ZoneFlag.ENTITY_SPAWN, (UUID) null)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;

        if (query.isDenied(victim.getLocation(), ZoneFlag.PVP, attacker.getUniqueId())) {
            e.setCancelled(true);
        }
    }
}

