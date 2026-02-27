package io.github.minehollow.bestiary.monster;

import io.github.minehollow.bestiary.BestiaryPlugin;
import io.github.minehollow.bestiary.event.MonsterDamageEvent;
import io.github.minehollow.bestiary.event.MonsterDeathEvent;
import io.github.minehollow.bestiary.event.MonsterSpawnEvent;
import io.github.minehollow.bestiary.model.CustomMonsterModel;
import io.github.minehollow.bestiary.model.CustomMonsterModelManager;
import io.github.minehollow.bestiary.monster.ability.AbilityManager;
import io.github.minehollow.bestiary.monster.goal.MobGoalManager;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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
    private final AbilityManager abilityManager;
    private final Random random = new Random();

    // Cache duplo para acesso rápido tanto por lógica de jogo (UUID) quanto por pacotes (Entity ID)
    private final Map<UUID, ActiveMonster> activeByUUID = new ConcurrentHashMap<>();
    private final Int2ObjectMap<ActiveMonster> activeById = new Int2ObjectOpenHashMap<>();

    private final NamespacedKey modelIdKey;

    public MonsterManager(@NotNull BestiaryPlugin plugin, @NotNull CustomMonsterModelManager modelManager,
                          @NotNull AbilityManager abilityManager) {
        this.plugin = plugin;
        this.modelManager = modelManager;
        this.abilityManager = abilityManager;
        this.modelIdKey = new NamespacedKey(MonsterStats.PDC_NAMESPACE, MonsterStats.KEY_MODEL_ID);
    }

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
        stats.setScale(model.getScale());
        MonsterHologram holo = new MonsterHologram(entity, model.getScale());
        holo.update(model.getDisplayName(), level, maxHealth, maxHealth);

        entity.setPersistent(false);

        ActiveMonster active = new ActiveMonster(entity, stats, holo, model);

        // Apply custom AI goals + ability casting goals based on the model's behavior profile
        MobGoalManager.applyBehavior(entity, model.getBehavior(), active, abilityManager);

        // Registro nos dois caches
        activeByUUID.put(entity.getUniqueId(), active);
        activeById.put(entity.getEntityId(), active);

        return active;
    }

    public double handleDamage(@NotNull LivingEntity entity, double rawDamage,
                               @Nullable EntityDamageEvent.DamageCause cause) {
        ActiveMonster active = activeByUUID.get(entity.getUniqueId());
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
        ActiveMonster active = activeByUUID.remove(entity.getUniqueId());
        if (active == null) return;

        activeById.remove(entity.getEntityId());
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
        ActiveMonster active = activeByUUID.remove(uuid);
        if (active != null) {
            activeById.remove(active.entity().getEntityId());
            active.hologram().remove();
        }
    }

    public void removeAll() {
        new ArrayList<>(activeByUUID.keySet()).forEach(this::removeSilently);
    }

    public boolean isCustomMonster(@NotNull LivingEntity entity) {
        return activeByUUID.containsKey(entity.getUniqueId())
               || MonsterStats.isCustomMonster(entity, modelIdKey);
    }

    public @Nullable ActiveMonster getActive(@Nullable UUID uuid) {
        return uuid == null ? null : activeByUUID.get(uuid);
    }

    // Novo método para o MonsterPacketListener buscar por ID de pacote
    public @Nullable ActiveMonster getActive(int entityId) {
        return activeById.get(entityId);
    }

    public @NotNull Collection<ActiveMonster> getAllActive() {
        return Collections.unmodifiableCollection(activeByUUID.values());
    }

    public @NotNull NamespacedKey getModelIdKey() {
        return modelIdKey;
    }

    public @NotNull AbilityManager getAbilityManager() {
        return abilityManager;
    }

    void restoreFromEntity(@NotNull LivingEntity entity, @NotNull MonsterStats stats) {
        String modelId = stats.getModelId();
        if (modelId == null) return;

        CustomMonsterModel model = modelManager.getModelIfPresent(modelId);
        if (model == null) {
            plugin.getLogger().warning("Loaded entity with unknown model '" + modelId + "', skipping.");
            return;
        }

        MonsterHologram holo = new MonsterHologram(entity, model.getScale());
        holo.update(model.getDisplayName(), stats.getLevel(), stats.getHealth(), stats.getMaxHealth());
        stats.setScale(model.getScale());

        ActiveMonster active = new ActiveMonster(entity, stats, holo, model);

        // Re-apply custom AI goals + ability casting on chunk reload
        MobGoalManager.applyBehavior(entity, model.getBehavior(), active, abilityManager);

        activeByUUID.put(entity.getUniqueId(), active);
        activeById.put(entity.getEntityId(), active);
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

}