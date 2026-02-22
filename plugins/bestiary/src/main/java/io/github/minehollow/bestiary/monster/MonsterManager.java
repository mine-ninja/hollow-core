package io.github.minehollow.bestiary.monster;

import io.github.minehollow.bestiary.BestiaryPlugin;
import io.github.minehollow.bestiary.event.MonsterDamageEvent;
import io.github.minehollow.bestiary.event.MonsterDeathEvent;
import io.github.minehollow.bestiary.event.MonsterSpawnEvent;
import io.github.minehollow.bestiary.model.CustomMonsterModel;
import io.github.minehollow.bestiary.model.CustomMonsterModelManager;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MonsterManager {

    private final BestiaryPlugin plugin;
    private final CustomMonsterModelManager modelManager;
    private final Random random = new Random();

    // ConcurrentHashMap elimina necessidade de synchronized em todo acesso
    private final Map<UUID, ActiveMonster> activeMonsters = new ConcurrentHashMap<>();

    private final NamespacedKey modelIdKey;

    public MonsterManager(@NotNull BestiaryPlugin plugin, @NotNull CustomMonsterModelManager modelManager) {
        this.plugin = plugin;
        this.modelManager = modelManager;
        this.modelIdKey = new NamespacedKey(MonsterStats.PDC_NAMESPACE, MonsterStats.KEY_MODEL_ID);
    }

    /**
     * Spawns a custom monster at the given location.
     * Thread-safe — may be called from any thread.
     */
    public @Nullable ActiveMonster spawn(@NotNull String modelId, @NotNull Location location) {
        CustomMonsterModel model = modelManager.getModelIfPresent(modelId);
        if (model == null) {
            plugin.getLogger().warning("Attempted to spawn unknown monster model: " + modelId);
            return null;
        }

        int level = model.getLevelRange().getRandomValue();

        MonsterSpawnEvent spawnEvent = new MonsterSpawnEvent(model, level, location);
        spawnEvent.callEvent();
        if (spawnEvent.isCancelled()) return null;

        return spawnMonster(model, level, spawnEvent.getSpawnLocation());
    }

    private ActiveMonster spawnMonster(CustomMonsterModel model, int level, Location location) {
        double maxHealth = model.getHealthPerLevelRange().random() * level;
        double damage = model.getDamagePerLevelRange().random() * level;
        double defense = model.getDefensePerLevelRange().random() * level;

        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, model.getEntityType());

        AttributeInstance attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) attribute.setBaseValue(maxHealth);
        entity.setHealth(maxHealth);
        entity.setCustomNameVisible(false);

        EntityEquipment eq = entity.getEquipment();
        if (eq != null) {
            eq.setHelmetDropChance(0f);
            eq.setChestplateDropChance(0f);
            eq.setLeggingsDropChance(0f);
            eq.setBootsDropChance(0f);
            eq.setItemInMainHandDropChance(0f);
            eq.setItemInOffHandDropChance(0f);
            for (Map.Entry<EquipmentSlot, ItemStack> entry : model.getEquipment().entrySet()) {
                eq.setItem(entry.getKey(), entry.getValue());
            }
        }

        MonsterStats stats = new MonsterStats(entity, model.getId(), level, maxHealth, damage, defense);
        MonsterHologram holo = new MonsterHologram(entity);
        holo.update(model.getDisplayName(), level, maxHealth, maxHealth);

        ActiveMonster active = new ActiveMonster(entity, stats, holo, model);
        activeMonsters.put(entity.getUniqueId(), active);
        return active;
    }

    public double handleDamage(@NotNull LivingEntity entity, double rawDamage,
                               @Nullable EntityDamageEvent.DamageCause cause) {
        ActiveMonster active = activeMonsters.get(entity.getUniqueId());
        if (active == null) return -1;

        MonsterDamageEvent damageEvent = new MonsterDamageEvent(entity, active, rawDamage, cause);
        damageEvent.callEvent();
        if (damageEvent.isCancelled()) return 0;

        double effective = active.stats().applyDamage(damageEvent.getFinalDamage());
        entity.setHealth(Math.max(0.001, active.stats().getHealth()));
        active.hologram().update(
            active.model().getDisplayName(),
            active.stats().getLevel(),
            active.stats().getHealth(),
            active.stats().getMaxHealth()
        );

        if (!active.stats().isAlive()) handleDeath(entity);

        return effective;
    }

    public void handleDeath(@NotNull LivingEntity entity) {
        ActiveMonster active = activeMonsters.remove(entity.getUniqueId());
        if (active == null) return;

        active.hologram().remove();

        List<ItemStack> drops = rollDrops(active);
        Player killer = entity.getKiller() instanceof Player p ? p : null;

        MonsterDeathEvent deathEvent = new MonsterDeathEvent(entity, active, killer, drops);
        deathEvent.callEvent();

        for (ItemStack item : deathEvent.getDrops()) {
            entity.getWorld().dropItemNaturally(entity.getLocation(), item);
        }

        entity.setHealth(0);
    }

    public void removeSilently(@NotNull UUID uuid) {
        ActiveMonster active = activeMonsters.remove(uuid);
        if (active != null) active.hologram().remove();
    }

    public void removeAll() {
        new ArrayList<>(activeMonsters.keySet()).forEach(this::removeSilently);
    }

    public boolean isCustomMonster(@NotNull LivingEntity entity) {
        return activeMonsters.containsKey(entity.getUniqueId())
               || MonsterStats.isCustomMonster(entity, modelIdKey);
    }

    public @Nullable ActiveMonster getActive(@NotNull UUID uuid) {
        return activeMonsters.get(uuid);
    }

    public @NotNull Collection<ActiveMonster> getAllActive() {
        return Collections.unmodifiableCollection(activeMonsters.values());
    }

    public @NotNull NamespacedKey getModelIdKey() {
        return modelIdKey;
    }

    void restoreFromEntity(@NotNull LivingEntity entity, @NotNull MonsterStats stats) {
        String modelId = stats.getModelId();
        if (modelId == null) return;

        CustomMonsterModel model = modelManager.getModelIfPresent(modelId);
        if (model == null) {
            plugin.getLogger().warning("Loaded entity with unknown model '" + modelId + "', skipping.");
            return;
        }

        MonsterHologram holo = new MonsterHologram(entity);
        holo.update(model.getDisplayName(), stats.getLevel(), stats.getHealth(), stats.getMaxHealth());
        activeMonsters.put(entity.getUniqueId(), new ActiveMonster(entity, stats, holo, model));
    }

    public void cleanup() {
        removeAll();
    }

    private List<ItemStack> rollDrops(ActiveMonster active) {
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<ItemStack, Double> entry : active.model().getPossibleDrops().entrySet()) {
            if (random.nextDouble() <= entry.getValue()) result.add(entry.getKey().clone());
        }
        return result;
    }

    public record ActiveMonster(
        LivingEntity entity,
        MonsterStats stats,
        MonsterHologram hologram,
        CustomMonsterModel model
    ) {
    }
}
