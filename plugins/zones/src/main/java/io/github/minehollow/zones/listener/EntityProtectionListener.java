package io.github.minehollow.zones.listener;

import io.github.minehollow.zones.ZoneQuery;
import io.github.minehollow.zones.model.ZoneFlag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Protects against entity spawns, mob spawns, and PvP.
 */
@RequiredArgsConstructor
public class EntityProtectionListener implements Listener {

    private final ZoneQuery query;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        final var loc = e.getLocation();
        if (e.getEntity() instanceof Monster) {
            if (query.isDenied(loc, ZoneFlag.MOB_SPAWN, (UUID) null)) {
                e.setCancelled(true);
                return;
            }
        }
        if (query.isDenied(loc, ZoneFlag.ENTITY_SPAWN, (UUID) null)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (query.isDenied(e.getEntity().getLocation(), ZoneFlag.DAMAGE, (UUID) null)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }
        if (!(e.getDamager() instanceof Player attacker)) {
            return;
        }

        if (query.isDenied(victim.getLocation(), ZoneFlag.DAMAGE, attacker.getUniqueId())) {
            e.setCancelled(true);
            return;
        }

        if (query.isDenied(victim.getLocation(), ZoneFlag.PVP, attacker.getUniqueId())) {
            e.setCancelled(true);
        }
    }
}

