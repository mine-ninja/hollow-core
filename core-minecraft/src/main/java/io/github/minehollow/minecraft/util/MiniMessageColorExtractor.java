package io.github.minehollow.minecraft.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MiniMessageColorExtractor {

    private static final Pattern NAMED_COLOR_PATTERN = Pattern.compile("<(dark_red|dark_green|dark_blue|dark_aqua|dark_purple|dark_gray|gold|gray|blue|green|aqua|red|light_purple|yellow|white|black)>");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[0-9A-Fa-f]{6}):(#[0-9A-Fa-f]{6})>");
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([0-9A-Fa-f]{6})>");

    // Cache com expiração de 5 segundos
    private static final Cache<@NotNull String, String> COLOR_CACHE = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofSeconds(5))
      .maximumSize(1000)
      .build();

    private MiniMessageColorExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Extrai a última cor/gradiente de uma string MiniMessage
     * Se a última formatação for um gradiente, retorna o gradiente completo
     *
     * @param miniMessageString String com formatação MiniMessage
     * @return A última cor/gradiente em formato MiniMessage (ex: "<#9D4EDD>" ou "<gradient:#E0AAFF:#9D4EDD>"), ou null se nenhuma for encontrada
     */
    public static String extractLastColorTag(String miniMessageString) {
        if (miniMessageString == null || miniMessageString.isEmpty()) {
            return null;
        }

        return COLOR_CACHE.get(miniMessageString, MiniMessageColorExtractor::calculateLastColorTag);
    }

    private static String calculateLastColorTag(String miniMessageString) {
        String lastColor = null;
        int lastIndex = -1;
        boolean isGradient = false;

        // Procura cores nomeadas
        Matcher namedMatcher = NAMED_COLOR_PATTERN.matcher(miniMessageString);
        while (namedMatcher.find()) {
            if (namedMatcher.start() > lastIndex) {
                lastIndex = namedMatcher.start();
                NamedTextColor color = NamedTextColor.NAMES.value(namedMatcher.group(1));
                lastColor = color != null ? String.format("<#%06X>", color.value()) : null;
                isGradient = false;
            }
        }

        // Procura gradientes (retorna o gradiente completo)
        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(miniMessageString);
        while (gradientMatcher.find()) {
            if (gradientMatcher.start() > lastIndex) {
                lastIndex = gradientMatcher.start();
                String color1 = gradientMatcher.group(1).toUpperCase();
                String color2 = gradientMatcher.group(2).toUpperCase();
                lastColor = "<gradient:" + color1 + ":" + color2 + ">";
                isGradient = true;
            }
        }

        // Procura cores hex
        Matcher hexMatcher = HEX_PATTERN.matcher(miniMessageString);
        while (hexMatcher.find()) {
            if (hexMatcher.start() > lastIndex) {
                lastIndex = hexMatcher.start();
                lastColor = "<#" + hexMatcher.group(1).toUpperCase() + ">";
                isGradient = false;
            }
        }

        return lastColor;
    }

    /**
     * Extrai a última cor de uma string MiniMessage, com fallback
     *
     * @param miniMessageString String com formatação MiniMessage
     * @param fallback          Cor padrão caso nenhuma seja encontrada (em formato MiniMessage, ex: "<#FFFFFF>")
     * @return A última cor encontrada, ou a cor de fallback
     */
    public static String extractLastColorTagOrDefault(String miniMessageString, String fallback) {
        String color = extractLastColorTag(miniMessageString);
        return color != null ? color : fallback;
    }

    /**
     * Limpa o cache de cores
     */
    public static void clearCache() {
        COLOR_CACHE.invalidateAll();
    }

    /**
     * Retorna o tamanho atual do cache
     */
    public static long getCacheSize() {
        return COLOR_CACHE.estimatedSize();
    }
}