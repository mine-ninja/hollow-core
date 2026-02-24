package io.github.minehollow.bestiary.monster;

import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Armazena e persiste os atributos de um monstro customizado. Removido @Data do Lombok — evita equals/hashCode/toString perigosos sobre Entity. NamespacedKeys
 * são cacheadas como constantes estáticas para evitar alocação repetida.
 */
@Getter
public class MonsterStats {

    public static final String PDC_NAMESPACE = "bestiary";
    public static final String KEY_MODEL_ID = "model_id";
    public static final String KEY_LEVEL = "level";
    public static final String KEY_HEALTH = "health";
    public static final String KEY_MAX_HEALTH = "max_health";
    public static final String KEY_DAMAGE = "damage";
    public static final String KEY_DEFENSE = "defense";

    private static final NamespacedKey KEY_NS_MODEL_ID;
    private static final NamespacedKey KEY_NS_LEVEL;
    private static final NamespacedKey KEY_NS_HEALTH;
    private static final NamespacedKey KEY_NS_MAX_HEALTH;
    private static final NamespacedKey KEY_NS_DAMAGE;
    private static final NamespacedKey KEY_NS_DEFENSE;

    static {
        KEY_NS_MODEL_ID = new NamespacedKey(PDC_NAMESPACE, KEY_MODEL_ID);
        KEY_NS_LEVEL = new NamespacedKey(PDC_NAMESPACE, KEY_LEVEL);
        KEY_NS_HEALTH = new NamespacedKey(PDC_NAMESPACE, KEY_HEALTH);
        KEY_NS_MAX_HEALTH = new NamespacedKey(PDC_NAMESPACE, KEY_MAX_HEALTH);
        KEY_NS_DAMAGE = new NamespacedKey(PDC_NAMESPACE, KEY_DAMAGE);
        KEY_NS_DEFENSE = new NamespacedKey(PDC_NAMESPACE, KEY_DEFENSE);
    }

    private final Entity entity;

    private double health;
    private double maxHealth;
    private double damage;
    private double defense;
    private final int level;
    private final @Nullable String modelId;

    /**
     * Construtor de spawn — persiste tudo de uma vez.
     */
    public MonsterStats(@NotNull Entity entity, @NotNull String modelId, int level,
                        double maxHealth, double damage, double defense) {
        this.entity = entity;
        this.modelId = modelId;
        this.level = level;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.damage = damage;
        this.defense = defense;

        persist();
    }

    /**
     * Construtor de restauração — lê do PDC.
     */
    public MonsterStats(@NotNull Entity entity, @NotNull NamespacedKeyFactory keys) {
        this.entity = entity;
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        this.modelId = pdc.get(keys.of(KEY_MODEL_ID), PersistentDataType.STRING);
        this.level = orDefault(pdc.get(keys.of(KEY_LEVEL), PersistentDataType.INTEGER), 1);
        this.maxHealth = orDefault(pdc.get(keys.of(KEY_MAX_HEALTH), PersistentDataType.DOUBLE), 20.0);
        this.health = orDefault(pdc.get(keys.of(KEY_HEALTH), PersistentDataType.DOUBLE), maxHealth);
        this.damage = orDefault(pdc.get(keys.of(KEY_DAMAGE), PersistentDataType.DOUBLE), 1.0);
        this.defense = orDefault(pdc.get(keys.of(KEY_DEFENSE), PersistentDataType.DOUBLE), 0.0);
    }

    // --- Getters simples ---

    public @NotNull Entity getEntity() {
        return entity;
    }

    public @Nullable String getModelId() {
        return modelId;
    }

    public boolean isAlive() {
        return health > 0;
    }

    // --- Setters com PDC individual (uso pontual) ---

    public void setHealth(double value) {
        this.health = Math.max(0, Math.min(value, maxHealth));
        entity.getPersistentDataContainer().set(KEY_NS_HEALTH, PersistentDataType.DOUBLE, this.health);
    }

    public void setMaxHealth(double value) {
        this.maxHealth = value;
        entity.getPersistentDataContainer().set(KEY_NS_MAX_HEALTH, PersistentDataType.DOUBLE, value);
    }

    public void setDamage(double value) {
        this.damage = value;
        entity.getPersistentDataContainer().set(KEY_NS_DAMAGE, PersistentDataType.DOUBLE, value);
    }

    public void setDefense(double value) {
        this.defense = value;
        entity.getPersistentDataContainer().set(KEY_NS_DEFENSE, PersistentDataType.DOUBLE, value);
    }

    public void setScale(double scale) {
        if (entity instanceof LivingEntity living) {
            AttributeInstance attribute = living.getAttribute(Attribute.SCALE);
            if (attribute != null) {
                attribute.setBaseValue(scale);
            }
        }
    }

    /**
     * Aplica dano com redução de defesa e persiste a HP.
     */
    public double applyDamage(double rawDamage) {
        double effective = Math.max(0, rawDamage - defense);
        setHealth(health - effective);
        return effective;
    }

    /**
     * Persiste todos os campos de uma vez — ideal no spawn.
     */
    public void persist() {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (modelId != null) {
            pdc.set(KEY_NS_MODEL_ID, PersistentDataType.STRING, modelId);
        }

        pdc.set(KEY_NS_LEVEL, PersistentDataType.INTEGER, level);
        pdc.set(KEY_NS_MAX_HEALTH, PersistentDataType.DOUBLE, maxHealth);
        pdc.set(KEY_NS_HEALTH, PersistentDataType.DOUBLE, health);
        pdc.set(KEY_NS_DAMAGE, PersistentDataType.DOUBLE, damage);
        pdc.set(KEY_NS_DEFENSE, PersistentDataType.DOUBLE, defense);
    }

    public static boolean isCustomMonster(@NotNull Entity entity, @NotNull NamespacedKey modelIdKey) {
        return entity.getPersistentDataContainer().has(modelIdKey, PersistentDataType.STRING);
    }

    private static <T> T orDefault(@Nullable T value, T def) {
        return value != null ? value : def;
    }

    @FunctionalInterface
    public interface NamespacedKeyFactory {
        NamespacedKey of(String key);
    }
}
