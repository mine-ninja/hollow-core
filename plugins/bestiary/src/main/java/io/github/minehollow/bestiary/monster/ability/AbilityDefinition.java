package io.github.minehollow.bestiary.monster.ability;

import lombok.Getter;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable ability template parsed once from config.
 * Shared across all entities of the same monster model — zero per-entity overhead.
 *
 * <p>Example config block:
 * <pre>
 * abilities:
 *   fireball:
 *     display-name: "&c🔥 Fireball"
 *     type: PROJECTILE
 *     damage: "8.0-14.0"
 *     cooldown: 5000          # ms
 *     range: 20.0
 *     speed: 1.2              # projectile speed (blocks/tick)
 *     particle: FLAME
 *     sound: ENTITY_BLAZE_SHOOT
 *   ground_slam:
 *     display-name: "&6💥 Ground Slam"
 *     type: AOE
 *     damage: "10.0-18.0"
 *     cooldown: 8000
 *     range: 5.0
 *     radius: 4.0
 *     particle: EXPLOSION
 *     sound: ENTITY_GENERIC_EXPLODE
 *   shadow_bolt:
 *     display-name: "&5⚡ Shadow Bolt"
 *     type: TARGETED
 *     damage: "6.0-10.0"
 *     cooldown: 6000
 *     range: 16.0
 *     particle: WITCH
 *     sound: ENTITY_ILLUSIONER_CAST_SPELL
 * </pre>
 */
@Getter
public final class AbilityDefinition {

    private final String id;
    private final String displayName;
    private final AbilityType type;
    private final DamageRange damageRange;

    /** Cooldown in milliseconds. */
    private final long cooldown;

    /** Max cast range (distance to target). */
    private final double range;

    /** AOE radius (only for AOE type). */
    private final double radius;

    /** Projectile speed in blocks per tick (only for PROJECTILE type). */
    private final double speed;

    /** Particle used for the effect. */
    private final @Nullable Particle particle;

    /** Sound played on cast. */
    private final @Nullable Sound sound;

    // ── Pre-computed constants for hot-path checks ──
    private final double rangeSquared;
    private final double radiusSquared;

    public AbilityDefinition(String id, String displayName, AbilityType type,
                             DamageRange damageRange, long cooldown,
                             double range, double radius, double speed,
                             @Nullable Particle particle, @Nullable Sound sound) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.damageRange = damageRange;
        this.cooldown = cooldown;
        this.range = range;
        this.radius = radius;
        this.speed = speed;
        this.particle = particle;
        this.sound = sound;
        this.rangeSquared = range * range;
        this.radiusSquared = radius * radius;
    }

    /**
     * Parse a single ability from a config section.
     */
    public static @NotNull AbilityDefinition readFromSection(@NotNull String id, @NotNull ConfigurationSection section) {
        String displayName = section.getString("display-name", id);
        AbilityType type = AbilityType.valueOf(section.getString("type", "TARGETED").toUpperCase());
        DamageRange damageRange = DamageRange.parse(section.getString("damage", "0-0"));
        long cooldown = section.getLong("cooldown", 5000L);
        double range = section.getDouble("range", 10.0);
        double radius = section.getDouble("radius", 3.0);
        double speed = section.getDouble("speed", 0.8);
        Particle particle = safeParticle(section.getString("particle", null));
        Sound sound = safeSound(section.getString("sound", null));

        return new AbilityDefinition(id, displayName, type, damageRange, cooldown, range, radius, speed, particle, sound);
    }

    /**
     * Parse all abilities from a parent "abilities" section.
     * Returns an unmodifiable list for safe sharing across entities.
     */
    public static @NotNull List<AbilityDefinition> readAllFromSection(@Nullable ConfigurationSection section) {
        if (section == null) return Collections.emptyList();
        List<AbilityDefinition> list = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection sub = section.getConfigurationSection(key);
            if (sub != null) list.add(readFromSection(key, sub));
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Write this ability to a config section.
     */
    @SuppressWarnings("removal")
    public void writeToSection(@NotNull ConfigurationSection parent) {
        ConfigurationSection section = parent.createSection(id);
        section.set("display-name", displayName);
        section.set("type", type.name());
        section.set("damage", damageRange.toString());
        section.set("cooldown", cooldown);
        section.set("range", range);
        section.set("radius", radius);
        section.set("speed", speed);
        if (particle != null) section.set("particle", particle.name());
        if (sound != null) section.set("sound", sound.key().value());
    }

    // ── Safe enum parsing ──

    private static @Nullable Particle safeParticle(@Nullable String name) {
        if (name == null) return null;
        try { return Particle.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    public static @Nullable Sound safeSound(@Nullable String name) {
        if (name == null) return null;
        try {
            // Support both "entity.blaze.shoot" and "ENTITY_BLAZE_SHOOT" formats
            String key = name.toLowerCase().replace('_', '.');
            Sound sound = Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(key));
            if (sound != null) return sound;
            // Try the raw key as-is
            return Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
        } catch (Exception e) { return null; }
    }
}

