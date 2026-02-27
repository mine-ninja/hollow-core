package io.github.minehollow.bestiary.monster.ability;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Central registry mapping {@link AbilityType} → {@link AbilityExecutor}.
 *
 * <p>Built-in types ({@code PROJECTILE}, {@code AOE}, {@code TARGETED}) are registered
 * automatically by {@link AbilityManager}. Custom types can be registered at any time
 * and take effect immediately for all future casts — even on already-spawned monsters.</p>
 *
 * <h3>Registering a custom ability:</h3>
 * <pre>{@code
 * AbilityExecutorRegistry registry = abilityManager.executors();
 *
 * // Simple lambda — single target
 * registry.register(AbilityType.METEOR, ctx -> {
 *     Player target = ctx.closestTarget();
 *     if (target == null) return;
 *     // ... spawn fake falling entity, particles, delayed AOE damage
 * });
 *
 * // Multi-target
 * registry.register(AbilityType.CHAIN_LIGHTNING, ctx -> {
 *     for (Player target : ctx.targets()) {
 *         // ... beam + damage per target
 *     }
 * });
 *
 * // Override a built-in type
 * registry.register(AbilityType.PROJECTILE, MyBetterProjectile::execute);
 * }</pre>
 */
public final class AbilityExecutorRegistry {

    private final Map<AbilityType, AbilityExecutor> executors = new EnumMap<>(AbilityType.class);
    private final Logger logger;

    public AbilityExecutorRegistry(@NotNull Logger logger) {
        this.logger = logger;
    }

    /**
     * Register (or replace) the executor for an ability type.
     *
     * @param type     the ability type to bind
     * @param executor the executor implementation
     */
    public void register(@NotNull AbilityType type, @NotNull AbilityExecutor executor) {
        AbilityExecutor previous = executors.put(type, executor);
        if (previous != null) {
            logger.info("[Abilities] Replaced executor for type: " + type.name());
        } else {
            logger.info("[Abilities] Registered executor for type: " + type.name());
        }
    }

    /**
     * Unregister the executor for an ability type.
     *
     * @param type the ability type to unbind
     * @return true if an executor was removed
     */
    public boolean unregister(@NotNull AbilityType type) {
        return executors.remove(type) != null;
    }

    /**
     * Get the executor for an ability type, or null if not registered.
     */
    public @Nullable AbilityExecutor get(@NotNull AbilityType type) {
        return executors.get(type);
    }

    /**
     * Check if an executor is registered for the given type.
     */
    public boolean isRegistered(@NotNull AbilityType type) {
        return executors.containsKey(type);
    }
}

