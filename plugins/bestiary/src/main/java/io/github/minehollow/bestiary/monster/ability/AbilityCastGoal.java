package io.github.minehollow.bestiary.monster.ability;

import io.github.minehollow.bestiary.monster.ActiveMonster;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.List;

/**
 * NMS pathfinder goal that makes a mob cast abilities on its current attack target.
 *
 * <p><b>Flow:</b></p>
 * <ol>
 *   <li>{@link #canUse()} — checks if the mob has a living target AND any ability is off cooldown + in range.
 *       Throttled to check every {@value CHECK_INTERVAL} ticks (0.5s) to reduce CPU cost.</li>
 *   <li>{@link #start()} — casts the selected ability immediately via {@link AbilityManager}.</li>
 *   <li>{@link #canContinueToUse()} — always false (single-shot execution).</li>
 * </ol>
 *
 * <p><b>Optimization:</b></p>
 * <ul>
 *   <li>Uses {@code Flag.MOVE} and {@code Flag.LOOK} — mob pauses movement during cast.</li>
 *   <li>Distance check uses squared math (no sqrt).</li>
 *   <li>Cooldown tracker is a primitive long[] — zero GC pressure.</li>
 *   <li>Only inserted if the model has abilities — otherwise this goal is never created.</li>
 * </ul>
 */
public class AbilityCastGoal extends Goal {

    private static final int CHECK_INTERVAL = 10; // ticks (0.5s)

    private final Mob mob;
    private final ActiveMonster monster;
    private final AbilityManager abilityManager;
    private final List<AbilityDefinition> abilities;
    private final AbilityCooldownTracker cooldowns;

    /** Index of the ability selected this cycle. */
    private int selectedIndex = -1;
    private int checkCooldown = 0;

    public AbilityCastGoal(@NotNull Mob mob,
                           @NotNull ActiveMonster monster,
                           @NotNull AbilityManager abilityManager) {
        this.mob = mob;
        this.monster = monster;
        this.abilityManager = abilityManager;
        this.abilities = monster.model().getAbilities();
        this.cooldowns = monster.abilityCooldowns();
        // Mob stops moving and looks at target during cast
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        // Throttle
        if (--checkCooldown > 0) return false;
        checkCooldown = CHECK_INTERVAL;

        net.minecraft.world.entity.LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return false;

        double distSq = mob.distanceToSqr(target);
        selectedIndex = cooldowns.findReady(abilities, distSq);
        return selectedIndex >= 0;
    }

    @Override
    public boolean canContinueToUse() {
        return false; // single-shot
    }

    @Override
    public void start() {
        net.minecraft.world.entity.LivingEntity target = mob.getTarget();
        if (target == null || selectedIndex < 0) return;

        // Look at target
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Mark cooldown
        cooldowns.markCast(selectedIndex);

        // Delegate execution to AbilityManager
        abilityManager.cast(monster, selectedIndex);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return false; // everything happens in start()
    }
}

