package io.github.minehollow.ranks.util;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

public class ExpressionProcessor {
    private static final Pattern PATTERN = Pattern.compile(
      "\\{level\\s*([+\\-*/])?\\s*(\\d+)?}"
    );

    public static String process(@NotNull String input, int level) {
        return PATTERN.matcher(input).replaceAll(match -> {
            if (match.group(1) == null) {
                return String.valueOf(level);
            }

            String operator = match.group(1);
            int modifier = Integer.parseInt(match.group(2));

            int result = switch (operator) {
                case "+" -> level + modifier;
                case "-" -> level - modifier;
                case "*" -> level * modifier;
                case "/" -> (modifier != 0) ? level / modifier : 0;
                default -> level;
            };

            return String.valueOf(result);
        });
    }
}