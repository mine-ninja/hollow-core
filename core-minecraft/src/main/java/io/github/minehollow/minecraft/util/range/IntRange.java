package io.github.minehollow.minecraft.util.range;

import org.jetbrains.annotations.NotNull;

import java.util.function.IntConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record IntRange(int min, int max) {

    private static final Pattern RANGE_PATTERN = Pattern.compile("^\\(?(-?\\d+)\\)?(?:\\s*-\\s*\\(?(-?\\d+)\\)?)?$");

    // suported formats: "5", "5-10", "(5)", "(5)-(10)", "5 - 10", "(5) - (10)"
    public static IntRange parseString(@NotNull String rangeStr) {
        Matcher matcher = RANGE_PATTERN.matcher(rangeStr.trim());
        if (matcher.matches()) {
            int min = Integer.parseInt(matcher.group(1));

            if (matcher.group(2) == null) {
                return single(min);
            }

            int max = Integer.parseInt(matcher.group(2));
            return of(min, max);
        }

        throw new IllegalArgumentException("Invalid range format: " + rangeStr);
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
