package io.github.minehollow.minecraft.util.range;

import org.jetbrains.annotations.NotNull;

import java.util.function.IntConsumer;

public record IntRange(int min, int max) {

    // Transforma uma string tipo "5-10" ou "7" em um IntRange
    public static IntRange parseString(@NotNull String rangeStr) {
        final var parts = rangeStr.split("-");
        if (parts.length == 1) {
            int value = Integer.parseInt(parts[0].trim());
            return single(value);
        } else if (parts.length == 2) {
            int min = Integer.parseInt(parts[0].trim());
            int max = Integer.parseInt(parts[1].trim());
            return of(min, max);
        } else {
            throw new IllegalArgumentException("Invalid range format: " + rangeStr);
        }
    }

    public static IntRange of(int min, int max) {
        return new IntRange(min, max);
    }

    public static IntRange single(int value) {
        return new IntRange(value, value);
    }

    public IntRange {
        if (min > max) {
            throw new IllegalArgumentException("Min value cannot be greater than max value.");
        }
    }

    public boolean contains(int value) {
        return value >= min && value <= max;
    }

    public boolean isSingleValue() {
        return min == max;
    }

    public int getRandomValue() {
        if (min == max) {
            return min;
        }
        return min + (int) (Math.random() * (max - min + 1));
    }

    public void forEach(@NotNull IntConsumer action) {
        if (min == max) {
            action.accept(min);
            return;
        }

        for (int i = min; i <= max; i++) {
            action.accept(i);
        }
    }

    @Override
    public @NotNull String toString() {
        return (min == max) ? String.valueOf(min) : min + "-" + max;
    }
}
