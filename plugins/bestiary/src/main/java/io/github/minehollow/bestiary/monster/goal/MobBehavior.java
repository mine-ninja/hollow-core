package io.github.minehollow.bestiary.monster.goal;

import java.util.*;
import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

/**
 * Lightweight, immutable behavior profile for a monster model.
 * Parsed once from config and reused for every spawn of that model.
 *
 * <p>Example config:
 * <pre>
 * behavior:
 *   aggressive: true
 *   target-range: 16.0
 *   attack-speed: 1.0
 *   retaliate: true
 *   wander: true
 *   float: true
 *   look-at-player: true
 *   look-around: true
 * </pre>
 */
@Getter
public final class MobBehavior {

    /** Whether this mob actively targets nearby players. */
    private final boolean aggressive;

    /** Max distance at which the mob acquires a target. */
    private final double targetRange;

    /** Movement speed multiplier when attacking (1.0 = default). */
    private final double attackSpeed;

    /** Whether the mob retaliates when hit (HURT_BY_TARGET). */
    private final boolean retaliate;

    /** Whether the mob wanders when idle. */
    private final boolean wander;

    /** Whether the mob floats on water. */
    private final boolean floatOnWater;

    /** Whether the mob looks at nearby players. */
    private final boolean lookAtPlayer;

    /** Whether the mob randomly looks around. */
    private final boolean lookAround;

    private MobBehavior(boolean aggressive, double targetRange, double attackSpeed,
                        boolean retaliate, boolean wander, boolean floatOnWater,
                        boolean lookAtPlayer, boolean lookAround) {
        this.aggressive = aggressive;
        this.targetRange = targetRange;
        this.attackSpeed = attackSpeed;
        this.retaliate = retaliate;
        this.wander = wander;
        this.floatOnWater = floatOnWater;
        this.lookAtPlayer = lookAtPlayer;
        this.lookAround = lookAround;
    }

    /**
     * Default aggressive behavior — targets players, retaliates, wanders.
     */
    public static final MobBehavior DEFAULT_AGGRESSIVE = new MobBehavior(
        true, 16.0, 1.0, true, true, true, true, true
    );

    /**
     * Default passive behavior — no targeting, still wanders.
     */
    public static final MobBehavior DEFAULT_PASSIVE = new MobBehavior(
        false, 0, 0, false, true, true, true, true
    );

    /**
     * Parse from a config section. Missing keys fall back to aggressive defaults.
     */
    public static @NotNull MobBehavior readFromSection(@NotNull ConfigurationSection section) {
        boolean aggressive = section.getBoolean("aggressive", true);
        double targetRange = section.getDouble("target-range", 16.0);
        double attackSpeed = section.getDouble("attack-speed", 1.0);
        boolean retaliate = section.getBoolean("retaliate", true);
        boolean wander = section.getBoolean("wander", true);
        boolean floatOnWater = section.getBoolean("float", true);
        boolean lookAtPlayer = section.getBoolean("look-at-player", true);
        boolean lookAround = section.getBoolean("look-around", true);

        return new MobBehavior(aggressive, targetRange, attackSpeed,
            retaliate, wander, floatOnWater, lookAtPlayer, lookAround);
    }

    /**
     * Serialize to a config section.
     */
    public void writeToSection(@NotNull ConfigurationSection section) {
        section.set("aggressive", aggressive);
        section.set("target-range", targetRange);
        section.set("attack-speed", attackSpeed);
        section.set("retaliate", retaliate);
        section.set("wander", wander);
        section.set("float", floatOnWater);
        section.set("look-at-player", lookAtPlayer);
        section.set("look-around", lookAround);
    }

    /**
     * Produces the ordered set of MobGoalTypes this behavior enables.
     */
    public @NotNull EnumSet<MobGoalType> enabledGoals() {
        EnumSet<MobGoalType> set = EnumSet.noneOf(MobGoalType.class);

        if (floatOnWater) set.add(MobGoalType.FLOAT_ON_WATER);
        if (aggressive)   set.add(MobGoalType.MELEE_ATTACK);
        if (wander)       set.add(MobGoalType.RANDOM_STROLL);
        if (lookAtPlayer) set.add(MobGoalType.LOOK_AT_PLAYER);
        if (lookAround)   set.add(MobGoalType.RANDOM_LOOK_AROUND);
        if (aggressive)   set.add(MobGoalType.TARGET_NEAREST_PLAYER);
        if (retaliate)    set.add(MobGoalType.HURT_BY_TARGET);

        return set;
    }
}

