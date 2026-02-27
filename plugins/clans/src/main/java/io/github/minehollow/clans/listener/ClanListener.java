package io.github.minehollow.clans.listener;

import io.github.minehollow.clans.config.MessageConfig;
import io.github.minehollow.clans.model.Clan;
import io.github.minehollow.clans.service.ClanService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Blocks PvP between clan mates when friendly fire is disabled.
 */
@RequiredArgsConstructor
public class ClanListener implements Listener {

    private final ClanService clanService;

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDamagePlayer(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (victim.getUniqueId().equals(attacker.getUniqueId())) return;

        Clan attackerClan = clanService.getByPlayer(attacker.getUniqueId());
        if (attackerClan == null) return;
        if (!attackerClan.isMember(victim.getUniqueId())) return;

        // Same clan — check friendly fire setting
        if (!attackerClan.isFriendlyFire()) {
            event.setCancelled(true);
            MessageConfig cfg = MessageConfig.getInstance();
            if (cfg != null) {
                attacker.sendMessage(cfg.get("friendly-fire-blocked"));
            }
        }
    }
}

