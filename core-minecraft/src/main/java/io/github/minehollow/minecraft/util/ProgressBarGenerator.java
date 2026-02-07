package io.github.minehollow.minecraft.util;

import io.github.minehollow.minecraft.util.message.StringUtils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public final class ProgressBarGenerator {

    // check if string starts with "<gradient:" and ends with ">"
    private static final Pattern GRADIENT_MINIMESSAGE_START_PATTERN = Pattern.compile("^<gradient:[^>]+>$");
    private static final String GRADIENT_CLOSE_TAG = "</gradient>";


    private static boolean isGradientMinimessage(@NotNull String input) {
        return GRADIENT_MINIMESSAGE_START_PATTERN.matcher(input).matches();
    }

    private static final char DEFAULT_FILLED = '█';
    private static final char DEFAULT_EMPTY = '░';

    private ProgressBarGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }


    public static String generateStr(
      double current,
      double max,
      int size,
      char filled,
      char empty,
      String filledColor,
      String emptyColor
    ) {
        if (size < 1 || max <= 0) {
            throw new IllegalArgumentException("Tamanho e máximo devem ser maiores que 0");
        }


        int filledCount = (int) ((current / max) * size);
        filledCount = Math.min(Math.max(filledCount, 0), size);
        int emptyCount = size - filledCount;

        StringBuilder sb = new StringBuilder(size + filledColor.length() + emptyColor.length());

        if (filledCount > 0) {
            sb.append(filledColor);
            sb.append(String.valueOf(filled).repeat(filledCount));

            if (isGradientMinimessage(filledColor)) {
                sb.append(GRADIENT_CLOSE_TAG);
            }
        }

        if (emptyCount > 0) {
            sb.append(emptyColor);
            sb.append(String.valueOf(empty).repeat(emptyCount));

            if (isGradientMinimessage(emptyColor)) {
                sb.append(GRADIENT_CLOSE_TAG);
            }
        }

        return sb.toString();
    }

    public static Component generate(
      double current,
      double max,
      int size,
      char filled,
      char empty,
      String filledColor,
      String emptyColor
    ) {
        if (size < 1 || max <= 0) {
            throw new IllegalArgumentException("Tamanho e máximo devem ser maiores que 0");
        }

        int filledCount = (int) ((current / max) * size);
        filledCount = Math.min(Math.max(filledCount, 0), size);
        int emptyCount = size - filledCount;

        StringBuilder sb = new StringBuilder(size + filledColor.length() + emptyColor.length());

        if (filledCount > 0) {
            sb.append(filledColor);
            sb.append(String.valueOf(filled).repeat(filledCount));
        }

        if (emptyCount > 0) {
            sb.append(emptyColor);
            sb.append(String.valueOf(empty).repeat(emptyCount));
        }

        return StringUtils.formatString(sb.toString());
    }

    public static Component generate(double current, double max, int size) {
        return generate(current, max, size, DEFAULT_FILLED, DEFAULT_EMPTY, "§a", "§7");
    }

    public static Component withPercentage(double current, double max, int size,
                                           char filled, char empty,
                                           String filledColor, String emptyColor) {
        Component bar = generate(current, max, size, filled, empty, filledColor, emptyColor);
        int pct = (int) ((current / max) * 100);
        return bar.append(StringUtils.formatString(" <gray>(" + pct + "%)"));
    }

    public static Component withPercentage(double current, double max, int size) {
        return withPercentage(current, max, size, DEFAULT_FILLED, DEFAULT_EMPTY, "§a", "§7");
    }
}