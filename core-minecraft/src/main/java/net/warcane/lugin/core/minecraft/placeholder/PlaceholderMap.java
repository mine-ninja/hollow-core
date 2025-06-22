package net.warcane.lugin.core.minecraft.placeholder;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class PlaceholderMap {


    private static final Set<TextPlaceholder> TEXT_PLACEHOLDERS = new HashSet<>();
    private static final Map<String, Pattern> CACHED_PATTERN = new LinkedHashMap<>();

    /**
     * Registra um placeholder de texto no sistema.
     * Se já existir um placeholder com o mesmo tag, lança uma exceção.
     *
     * @param textPlaceholder O placeholder a ser registrado.
     * @throws IllegalArgumentException Se já existir um placeholder com o mesmo tag.
     */
    public static void registerPlaceholder(@NotNull TextPlaceholder textPlaceholder) {
        var existsByTag = TEXT_PLACEHOLDERS.stream().anyMatch(it -> it.getTag().equals(textPlaceholder.getTag()));
        if (existsByTag) {
            throw new IllegalArgumentException("Placeholder with tag {" + textPlaceholder.getTag() + "} already exists.");
        }

        TEXT_PLACEHOLDERS.add(textPlaceholder);
        cachePattern(textPlaceholder.getTag());
    }

    /**
     * Cacheia o pattern compilado para um placeholder específico
     */
    private static void cachePattern(@NotNull String placeholder) {
        String escapedPlaceholder = Pattern.quote("{" + placeholder + "}");
        CACHED_PATTERN.put(placeholder, Pattern.compile(escapedPlaceholder));
    }


    public static String parseString(@NotNull String input) {
        return parseString(input, null);
    }

    /**
     * Versão otimizada do parseString usando patterns pre-cacheados
     */
    public static String parseString(@NotNull String input, @Nullable Player playerToReceiveMessage) {
        if (input.isEmpty() || !containsPlaceholderCharacters(input)) {
            return input;
        }

        return input.transform(text -> {
            for (TextPlaceholder textPlaceholder : TEXT_PLACEHOLDERS) {
                text = transformString(playerToReceiveMessage, text, textPlaceholder);
            }
            return text;
        });
    }

    private static String transformString(@Nullable Player playerToReceiveMessage, String text, TextPlaceholder textPlaceholder) {
        String tag = textPlaceholder.getTag();
        Pattern pattern = CACHED_PATTERN.get(tag);
        if (pattern == null) {
            return text;
        }

        if (playerToReceiveMessage != null && textPlaceholder instanceof PlayerTextPlaceholder playerPlaceholder) {
            String replacement = playerPlaceholder.getPlaceholderFunction().apply(playerToReceiveMessage);
            if (replacement != null) {
                text = pattern.matcher(text).replaceAll(replacement);
            }
        }

        if (textPlaceholder instanceof GlobalTextPlaceholder globalTextPlaceholder) {
            String replacement = globalTextPlaceholder.getPlaceholderSupplier().get();
            if (replacement != null) {
                text = pattern.matcher(text).replaceAll(replacement);
            }
        }
        return text;
    }

    /**
     * Limpa o cache de patterns (útil para testes ou limpeza de memória)
     */
    public static void clearPatternCache() {
        CACHED_PATTERN.clear();
    }

    /**
     * Remove um placeholder específico e seu pattern do cache
     */
    public static void removePlaceholder(@NotNull String tag) {
        TEXT_PLACEHOLDERS.removeIf(it -> it.getTag().equals(tag));
        CACHED_PATTERN.remove(tag);
    }

    /**
     * Retorna o número de patterns cacheados
     */
    public static int getCacheSize() {
        return CACHED_PATTERN.size();
    }

    private static boolean containsPlaceholderCharacters(@NotNull String input) {
        var indexOfOpenBrace = input.indexOf('{');
        var indexOfCloseBrace = input.indexOf('}');

        return indexOfOpenBrace != -1 && indexOfCloseBrace != -1 && indexOfOpenBrace < indexOfCloseBrace;
    }
}