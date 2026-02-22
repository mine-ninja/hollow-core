package io.github.minehollow.bestiary.monster;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.jetbrains.annotations.NotNull;

public class MonsterListener implements Listener {

    private final MonsterManager monsterManager;

    public MonsterListener(@NotNull MonsterManager monsterManager) {
        this.monsterManager = monsterManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event){
        final var player = event.getPlayer();
        for (final var active : monsterManager.getAllActive()) {
            active.hologram().addViewer(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerJoinEvent event){
        final var player = event.getPlayer();
        for (final var active : monsterManager.getAllActive()) {
            active.hologram().removeViewer(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (!monsterManager.isCustomMonster(living)) {
            return;
        }

        monsterManager.handleDamage(living, event.getFinalDamage(), event.getCause());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        final var monster = monsterManager.getActive(event.getEntity().getUniqueId());
        if (monster == null) {
            return;
        }

        monster.hologram().remove();

        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAttackMonster(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        MonsterManager.ActiveMonster active = monsterManager.getActive(living.getUniqueId());
        if (active == null) {
            return;
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        NamespacedKey modelIdKey = monsterManager.getModelIdKey();
        for (var entity : event.getEntities()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (monsterManager.getActive(entity.getUniqueId()) != null) {
                continue;
            }
            if (!MonsterStats.isCustomMonster(entity, modelIdKey)) {
                continue;
            }

            MonsterStats stats = new MonsterStats(
                living,
                key -> new NamespacedKey(MonsterStats.PDC_NAMESPACE, key)
            );

            if (stats.getModelId() == null) {
                continue;
            }
            monsterManager.restoreFromEntity(living, stats);
        }
    }
}
