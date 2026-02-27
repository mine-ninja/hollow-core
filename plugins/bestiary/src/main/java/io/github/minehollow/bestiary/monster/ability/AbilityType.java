package io.github.minehollow.bestiary.monster.ability;

/**
 * Delivery mechanism for a monster ability / spell.
 */
public enum AbilityType {

    /**
     * A non-entity projectile simulated via particle packets in a line.
     * Detects collision with player bounding boxes using ray-march.
     */
    PROJECTILE,

    /**
     * Area-of-effect radial damage around the caster.
     * Rendered as an expanding circular particle ring via packets.
     */
    AOE,

    /**
     * Unavoidable lock-on skill that targets the nearest/highest-aggro player.
     * Rendered as a "tether" or "strike" particle beam via packets.
     */
    TARGETED,

    /**
     * Melee charge / dash — the mob lunges toward the closest target,
     * dealing damage and knockback on impact. Rendered as a ground-level
     * particle trail along the dash path.
     */
    TACKLE
}

