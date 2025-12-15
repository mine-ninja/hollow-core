package net.warcane.lugin.core.minecraft.util.message;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.ParserDirective;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.function.Consumer;

/**
 * @author Rok, Pedro Lucas nmm. Created on 04/08/2025
 * @project factions-essentials
 */
@RequiredArgsConstructor
public enum AdventureFormatters {

    LUGIN_GREEN("l-green", "<#00fb9a>"),
    LUGIN_RED("l-red", "<#ff0000>"),
    LUGIN_BLUE("l-blue", "<#00aaff>"),
    LUGIN_YELLOW("l-yellow", "<#f7d000>"),
    LUGIN_PURPLE("l-purple", "<#a000ff>"),
    LUGIN_GRAY("l-gray", "<#b0b0b0>"),
    LUGIN_DARK_GRAY("l-dark-gray", "<#808080>"),
    LUGIN_GOLD("l-gold", "<#ffb700>"),
    LUGIN_WHITE("l-white", "<#ffffff>"),
    LUGIN_BLACK("l-black", "<#000000>"),
    LUGIN_CYAN("l-cyan", "<#00ffff>"),

    LUGIN_NEGATE("l-negate", "<#8a8a8a>[<#ff272a>✖<#8a8a8a>] <red>"),
    LUGIN_DENY("l-deny", LUGIN_NEGATE),
    LUGIN_CONFIRM("l-confirm", "<#8a8a8a>[<#00fb9a><bold>✔</bold><#8a8a8a>] <#00fb9a>"),
    LUGIN_ACCEPT("l-accept", LUGIN_CONFIRM),
    LUGIN_CHECK("l-check", LUGIN_CONFIRM),
    LUGIN_LOADING("l-loading", "<#8a8a8a>[<#f7e300>⌛<#8a8a8a>] <yellow>"),
    LUGIN_INFO("l-info", "<#8a8a8a>[<#00a2ff><bold>▶</bold><#8a8a8a>] <gray>"),
    LUGIN_SWORD("l-sword", "<#8a8a8a>[<white>🗡<#8a8a8a>] <gray>"),
    LUGIN_KILL("l-kill", LUGIN_SWORD),
    LUGIN_SKULL("l-skull", "<#8a8a8a>[<#ff272a>☠<#8a8a8a>] <red>"),
    LUGIN_ERROR("l-error", "<#8a8a8a>[<dark_red>☠<#8a8a8a>] <dark_red>"),
    LUGIN_MESSAGE("l-message", "<#8a8a8a>[<yellow>✉<#8a8a8a>] <yellow>"),
    ;

    private final String key;
    private final String value;
    private final boolean selfClosing;

    AdventureFormatters(String key, String value) {
        this.key = key;
        this.value = value;
        this.selfClosing = false;
    }

    AdventureFormatters(String key, AdventureFormatters other) {
        this.key = key;
        this.value = other.value;
        this.selfClosing = other.selfClosing;
    }

    public static void init() {
        init(StringUtils.miniMessage, mm -> StringUtils.miniMessage = mm);
    }

    private static void init(MiniMessage miniMessage, Consumer<MiniMessage> consumer) {
        MiniMessage.Builder builder = MiniMessage.builder();
        builder.tags(miniMessage.tags());


        MiniMessage finalMiniMessage = miniMessage;
        builder.editTags(t -> {
            for (AdventureFormatters keyObject : AdventureFormatters.values()) {
                String key = keyObject.key;
                String val = keyObject.value;
                if (finalMiniMessage.tags().has(key)) continue;
                TagResolver resolver;
                if (keyObject.selfClosing) {
                    resolver = TagResolver.resolver(key, (arg, ctx) -> Tag.selfClosingInserting(StringUtils.formatString(val)));
                } else {
                    resolver = TagResolver.resolver(key, (arg, ctx) -> Tag.preProcessParsed(val));
                }
                t.resolver(resolver);
            }

            // convert everything thas insite <small-caps> Text Here </small-caps> using SmallCpasMapping.toSmallCaps(String)
            TagResolver smallCapsResolver = TagResolver.resolver(
                "small-caps", (args, ctx) -> new SmallCapsTag()
            );
            t.resolver(smallCapsResolver);
        });
        miniMessage = builder.build();
        consumer.accept(miniMessage);
    }
}
