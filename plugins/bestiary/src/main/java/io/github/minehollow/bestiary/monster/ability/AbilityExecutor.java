package io.github.minehollow.bestiary.monster.ability;

import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for executing an ability cast.
 *
 * <p>Implementations are stateless lambdas or method references registered
 * in {@link AbilityExecutorRegistry}. They receive an {@link AbilityCastContext}
 * containing everything needed: caster, targets, VFX viewers, and the ability definition.</p>
 *
 * <h3>Usage — registering a custom ability:</h3>
 * <pre>{@code
 * AbilityExecutorRegistry registry = abilityManager.executors();
 *
 * // Lambda
 * registry.register(AbilityType.METEOR, ctx -> {
 *     Location impact = ctx.closestTarget().getLocation();
 *     // spawn fake falling entity, particles, delayed AOE damage...
 * });
 *
 * // Method reference
 * registry.register(AbilityType.BLACK_HOLE, BlackHoleAbility::execute);
 *
 * // Multi-target example
 * registry.register(AbilityType.CHAIN_LIGHTNING, ctx -> {
 *     for (Player target : ctx.targets()) {
 *         // beam to each target, apply damage
 *     }
 * });
 * }</pre>
 */
@FunctionalInterface
public interface AbilityExecutor {

    /**
     * Execute the ability.
     *
     * <p>Called on the main thread. The context's {@code targets()} list
     * contains all valid players in range (may be empty). Implementations
     * should check {@link AbilityCastContext#hasTargets()} before accessing targets.</p>
     *
     * @param ctx immutable cast context with caster, targets, viewers, and ability definition
     */
    void execute(@NotNull AbilityCastContext ctx);
}

