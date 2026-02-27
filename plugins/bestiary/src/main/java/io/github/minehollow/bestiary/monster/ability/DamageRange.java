package io.github.minehollow.bestiary.monster.ability;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Immutable min-max damage range. Uses primitive doubles — zero allocation on roll.
 *
 * @param min minimum damage (inclusive)
 * @param max maximum damage (inclusive)
 */
public record DamageRange(double min, double max) {

    public DamageRange {
        if (min < 0) throw new IllegalArgumentException("min cannot be negative: " + min);
        if (max < min) throw new IllegalArgumentException("max (" + max + ") < min (" + min + ")");
    }

    /**
     * Roll a random damage value in [min, max].
     */
    public double roll() {
        if (min == max) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    /**
     * Parse from "min-max" string format (e.g. "5.0-12.0").
     */
    public static DamageRange parse(String input) {
        if (input == null || input.isBlank()) return new DamageRange(0, 0);
        String[] parts = input.split("-", 2);
        double a = Double.parseDouble(parts[0].trim());
        double b = parts.length > 1 ? Double.parseDouble(parts[1].trim()) : a;
        return new DamageRange(Math.min(a, b), Math.max(a, b));
    }

    @Override
    public String toString() {
        return min + "-" + max;
    }
}

