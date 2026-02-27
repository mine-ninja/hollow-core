package io.github.minehollow.bestiary.monster.ability;

import io.github.minehollow.bestiary.monster.ActiveMonster;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable snapshot passed to every {@link AbilityExecutor} invocation.
 * Contains everything needed to render and apply a single ability cast.
 *
 * <p>Pre-computes commonly needed values (viewer list, origin, targets)
 * so executor implementations don't repeat the work.</p>
 *
 * <p><b>Multiple targets:</b> the {@code targets} list contains all valid
 * (survival-mode, alive) players within the ability's range at cast time.
 * The list is ordered by distance (closest first). It may be empty if no
 * players are in range — executors should handle that gracefully.</p>
 *
 * @param monster  the active monster casting the ability
 * @param ability  the ability definition being cast
 * @param caster   the Bukkit living entity (the mob)
 * @param targets  all valid players within range, sorted closest-first (never null, may be empty)
 * @param origin   the caster's eye location at cast time (cloned — safe to mutate)
 * @param viewers  pre-culled list of players within render distance for VFX
 */
public record AbilityCastContext(
    @NotNull ActiveMonster monster,
    @NotNull AbilityDefinition ability,
    @NotNull LivingEntity caster,
    @NotNull List<Player> targets,
    @NotNull Location origin,
    @NotNull List<Player> viewers
) {

    /**
     * @return the closest target, or null if no targets in range.
     */
    public @Nullable Player closestTarget() {
        return targets.isEmpty() ? null : targets.getFirst();
    }

    /**
     * @return true if there is at least one target in range.
     */
    public boolean hasTargets() {
        return !targets.isEmpty();
    }

    /**
     * @return the number of targets in range.
     */
    public int targetCount() {
        return targets.size();
    }

    /**
     * Roll damage from the ability's damage range.
     */
    public double rollDamage() {
        return ability.getDamageRange().roll();
    }

    /**
     * Convenience — play the ability's cast sound at the origin (if configured).
     */
    public void playCastSound() {
        if (ability.getSound() != null) {
            AbilityPacketEffects.sendSound(origin, ability.getSound(), viewers);
        }
    }

    /**
     * Convenience — send an impact burst particle at the given location (if configured).
     */
    public void playImpactAt(@NotNull Location location) {
        if (ability.getParticle() != null) {
            AbilityPacketEffects.sendImpactBurst(location, ability.getParticle(), viewers);
        }
    }
}

