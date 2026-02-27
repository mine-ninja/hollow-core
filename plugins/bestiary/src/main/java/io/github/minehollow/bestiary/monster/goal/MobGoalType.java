package io.github.minehollow.bestiary.monster.goal;

/**
 * All supported goal types for custom monsters.
 * Each type maps to one or more NMS pathfinder goals.
 */
public enum MobGoalType {

    /** Look at nearby players. Passive, cosmetic only. */
    LOOK_AT_PLAYER,

    /** Random idle looking around. */
    RANDOM_LOOK_AROUND,

    /** Wander randomly when idle. */
    RANDOM_STROLL,

    /** Float on water instead of sinking. */
    FLOAT_ON_WATER,

    /** Target the nearest player within range. */
    TARGET_NEAREST_PLAYER,

    /** Melee attack the current target. */
    MELEE_ATTACK,

    /** Hurt-by retaliation — target whoever hit this mob. */
    HURT_BY_TARGET
}

