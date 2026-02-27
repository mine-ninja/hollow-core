package io.github.minehollow.bestiary.monster.ability;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Per-entity cooldown tracker for abilities.
 * Uses a primitive long array indexed by ability list position — zero GC pressure.
 *
 * <p>One instance per {@link io.github.minehollow.bestiary.monster.ActiveMonster}.
 * The ability list comes from the model, so the index is stable for the entity's lifetime.</p>
 */
public final class AbilityCooldownTracker {

    /** Last-cast timestamps (System.currentTimeMillis) for each ability index. */
    private final long[] lastCastTimes;

    public AbilityCooldownTracker(int abilityCount) {
        this.lastCastTimes = new long[abilityCount];
        // All zeros → all abilities available immediately on spawn
    }

    /**
     * Check if the ability at the given index is off cooldown.
     */
    public boolean isReady(int index, @NotNull AbilityDefinition ability) {
        return System.currentTimeMillis() - lastCastTimes[index] >= ability.getCooldown();
    }

    /**
     * Mark the ability at the given index as just cast.
     */
    public void markCast(int index) {
        lastCastTimes[index] = System.currentTimeMillis();
    }

    /**
     * Finds the first ready ability whose range covers the given squared distance.
     *
     * @param abilities       the ordered ability list from the model
     * @param distanceSquared squared distance to the target
     * @return index of a ready ability, or -1 if none available
     */
    public int findReady(@NotNull List<AbilityDefinition> abilities, double distanceSquared) {
        for (int i = 0; i < abilities.size(); i++) {
            AbilityDefinition ability = abilities.get(i);
            if (distanceSquared <= ability.getRangeSquared() && isReady(i, ability)) {
                return i;
            }
        }
        return -1;
    }
}

