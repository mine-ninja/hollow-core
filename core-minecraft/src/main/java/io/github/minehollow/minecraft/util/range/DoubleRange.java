package io.github.minehollow.minecraft.util.range;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public record DoubleRange(double min, double max) {

    public static final char DEFAULT_SEPARATOR = '-';

    private static final String RANGE_REGEX_TEMPLATE =
        "^\\(?(-?\\d*\\.?\\d+)\\)?(?:\\s*%s\\s*\\(?(-?\\d*\\.?\\d+)\\)?)?$";

    public static @NotNull DoubleRange parseString(@NotNull String input) {
        return parseString(input, DEFAULT_SEPARATOR);
    }

    public static @NotNull DoubleRange parseString(@NotNull String input, char separator) {
        final var escapedSeparator = Pattern.quote(String.valueOf(separator));
        final var finalRegex = String.format(RANGE_REGEX_TEMPLATE, escapedSeparator);
        final var matcher = Pattern.compile(finalRegex).matcher(input.trim());

        if (matcher.matches()) {
            final double min = Double.parseDouble(matcher.group(1));
            if (matcher.group(2) == null) {
                return new DoubleRange(min, min);
            }

            final double max = Double.parseDouble(matcher.group(2));
            return new DoubleRange(min, max);
        }

        throw new IllegalArgumentException("Invalid range format: " + input);
    }

    public DoubleRange {
        if (min > max) {
            throw new IllegalArgumentException("Min value cannot be greater than max value.");
        }
    }


    public double random() {
        return min + Math.random() * (max - min);
    }

    public boolean isStatic() {
        return min == max;
    }

    public boolean isWithin(double value) {
        return value >= min && value <= max;
    }

    public double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String toString() {
        return isStatic() ? String.valueOf(min) : min + String.valueOf(DEFAULT_SEPARATOR) + max;
    }
}
