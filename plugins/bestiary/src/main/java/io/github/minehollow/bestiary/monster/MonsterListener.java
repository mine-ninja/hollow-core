package io.github.minehollow.bestiary.monster;

import io.github.minehollow.bestiary.display.DamageIndicator;
import io.github.minehollow.minecraft.event.tick.AsyncServerTickEvent;
import io.github.minehollow.minecraft.task.Tasks;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.jetbrains.annotations.NotNull;

public class MonsterListener implements Listener {

    private static final double PLAYER_PROXIMITY_RADIUS = 64.0;
    private final MonsterManager monsterManager;

    private final Queue<Runnable> mainThreadTasks = new ConcurrentLinkedQueue<>();

    public MonsterListener(@NotNull MonsterManager monsterManager) {
        this.monsterManager = monsterManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handleTick(AsyncServerTickEvent event) {
        for (final var activeMonster : monsterManager.getAllActive()) {
            if (activeMonster.isInactiveFor(30, TimeUnit.SECONDS)) {
                mainThreadTasks.add(() -> monsterManager.removeSilently(activeMonster.entity().getUniqueId()));
                continue;
            }

            if (activeMonster.hasPlayersAround(PLAYER_PROXIMITY_RADIUS)) {
                activeMonster.updateActivity();
            }
        }

        if (!mainThreadTasks.isEmpty()) {
            Tasks.runSync(() -> {
                Runnable task;
                while ((task = mainThreadTasks.poll()) != null) {
                    task.run();
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living) || !monsterManager.isCustomMonster(living)) {
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAttackMonster(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getDamager() instanceof Player p) || !(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        if (monsterManager.getActive(living.getUniqueId()) == null) {
            return;
        }

        boolean critical = p.getFallDistance() > 0 && !p.isOnGround();
        DamageIndicator.spawn(living, event.getFinalDamage(), critical);
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        NamespacedKey modelIdKey = monsterManager.getModelIdKey();
        for (var entity : event.getEntities()) {
            if (!(entity instanceof LivingEntity living) || monsterManager.getActive(entity.getUniqueId()) != null) {
                continue;
            }
            if (!MonsterStats.isCustomMonster(entity, modelIdKey)) {
                continue;
            }

            MonsterStats stats = new MonsterStats(living, key -> new NamespacedKey(MonsterStats.PDC_NAMESPACE, key));
            if (stats.getModelId() != null) {
                monsterManager.restoreFromEntity(living, stats);
            }
        }
    }
}