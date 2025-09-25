package net.warcane.lugin.core.minecraft.util.message;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * @author Rok, Pedro Lucas nmm. Created on 29/08/2024
 * @project BrPacks
 */
public class StringUtils {


    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Transforma uma String com as formatações de <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a> para {@link Component}.
     *
     * @param input String que será formatada.
     * @return {@link Component} formatado.
     */
    public static Component formatString(String input) {
        return format(input, false, false);
    }

    /**
     * Transforma uma String com as formatações de <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a> para {@link Component}.
     *
     * @param input    String que será formatada.
     * @param logError Se deve relatar o erro caso ocorra.
     * @return {@link Component} formatado.
     */
    public static Component formatString(String input, boolean logError, boolean forceSerialization) {
        return format(input, logError, forceSerialization);
    }

    /**
     * Transforma uma String com as formatações de <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a> para {@link Component}.
     *
     * @param input String que será formatada.
     * @return {@link Component} formatado.
     */
    public static Component text(String input) {
        return format(input, false, false);
    }

    public static Component multiText(@NotNull String... text) {
        return Arrays.stream(text)
          .map(StringUtils::text)
          .reduce(Component.empty(), (cur, next) -> cur.append(Component.newline()).append(next));
    }

    /**
     * Transforma uma String com as formatações de <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a> para {@link Component}.
     *
     * @param input    String que será formatada.
     * @param logError Se deve relatar o erro caso ocorra.
     * @return {@link Component} formatado.
     */
    public static Component text(String input, boolean logError, boolean forceSerialization) {
        return format(input, logError, forceSerialization);
    }

    /**
     * Transforma uma String com as formatações de <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a> para {@link Component}
     * removendo o {@link TextDecoration} "Italic".
     *
     * @param input String que será formatada.
     * @return {@link Component} formatado.
     */
    public static Component formItemName(String input) {
        return format(input, false, false).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Transforma uma String com as formatações de <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a> para {@link Component}
     * removendo o {@link TextDecoration} "Italic".
     *
     * @param input    String que será formatada.
     * @param logError Se deve relatar o erro caso ocorra.
     * @return {@link Component} formatado.
     */
    public static Component formItemName(String input, boolean logError, boolean forceSerialization) {
        return format(input, logError, forceSerialization).decoration(TextDecoration.ITALIC, false);
    }

    public static MiniMessage miniMessage = MiniMessage.miniMessage();

    /**
     * Transforma uma String com as formatações de <a href="https://docs.advntr.dev/minimessage/format.html">MiniMessage</a> para {@link Component}
     * removendo o {@link TextDecoration} "Italic".
     *
     * @param input String que será formatada.
     * @return {@link Component} formatado.
     */
    private static Component format(String input, boolean logError, boolean forceSerialization) {
        try {
            return miniMessage.deserialize(input);
        } catch (Exception e) {
            if (logError) {
                LOGGER.error("Error while formatting string: {}", input);
                LOGGER.error("Class that called: {}", Thread.currentThread().getStackTrace()[2].getClassName());
            }
            if (forceSerialization) {
                input = input.replace("§", "&");
                Component deserialize = miniMessage.deserialize(input);
                return LEGACY_SERIALIZER.deserialize(LEGACY_SERIALIZER.serialize(deserialize));
            }
            return Component.text(input);
        }
    }

    public static void send(Audience audience, String message) {
        audience.sendMessage(text(message));
    }

    public static void send(Audience audience, String message, boolean logError, boolean forceSerialization) {
        audience.sendMessage(text(message, logError, forceSerialization));
    }

    public static void send(Player player, String message) {
        send(BukkitPlatformPlugin.getInstance().adventure().player(player), message);
    }

    public static void send(Player player, String message, boolean logError, boolean forceSerialization) {
        send(BukkitPlatformPlugin.getInstance().adventure().player(player), message, logError, forceSerialization);
    }


    @NotNull
    public static TextReplacementConfig replace(@NotNull String a, @NotNull String b) {
        return replaceComponent(a, text(b));
    }

    @NotNull
    public static TextReplacementConfig replaceComponent(@NotNull String a, Component b) {
        return TextReplacementConfig.builder().match(a).replacement(b).build();
    }

    /**
     * Cria um {@link Title} com o título e subtítulo formatados.
     *
     * @param title    Título em {@link Component}.
     * @param subTitle Subtítulo em {@link Component}.
     * @param fadeIn   Tempo de fade in em milissegundos.
     * @param stay     Tempo de permanência em milissegundos.
     * @param fadeOut  Tempo de fade out em milissegundos.
     * @return {@link Title} formatado.
     */
    public static Title title(Component title, Component subTitle, long fadeIn, long stay, long fadeOut) {
        return Title.title(
                title,
                subTitle,
                Title.Times.times(Duration.ofMillis(fadeIn),
                        Duration.ofMillis(stay),
                        Duration.ofMillis(fadeOut)));
    }

    /**
     * Cria um {@link Title} com o título e subtítulo formatados.
     *
     * @param title    Título em {@link String}.
     * @param subtitle Subtítulo em {@link String}.
     * @param fadeIn   Tempo de fade in em milissegundos.
     * @param stay     Tempo de permanência em milissegundos.
     * @param fadeOut  Tempo de fade out em milissegundos.
     * @return
     */
    public static Title titleFormated(String title, String subtitle, long fadeIn, long stay, long fadeOut) {
        return title(StringUtils.text(title), StringUtils.text(subtitle), fadeIn, stay, fadeOut);
    }

    /**
     * Use este método para encontrar todas as opções que começam com a string fornecida, ignorando maiúsculas e minúsculas.
     * Recomendo usar este método para implementar a funcionalidade de auto-completar em comandos ou interfaces de usuário.
     */
    public static List<String> matchPartial(String toMatch, List<String> options) {
        String lowerToMatch = toMatch.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerToMatch))
                .toList();
    }


    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat() //&x&f&f&f&f&f&f
            .build();
}
