package io.github.minehollow.minecraft.util.message;

import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;


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
        try {
            MiniMessage.Builder builder = MiniMessage.builder();

            builder.tags(TagResolver.builder()
              .resolver(StandardTags.color())
              .resolver(StandardTags.decorations())
              .resolver(StandardTags.gradient())
              .build()
            ).postProcessor(component -> component.decoration(TextDecoration.ITALIC, false));

            builder.editTags(t -> {
                for (AdventureFormatters formatter : AdventureFormatters.values()) {
                    TagResolver resolver;
                    if (formatter.selfClosing) {
                        resolver = TagResolver.resolver(formatter.key, (arg, ctx) ->
                          Tag.selfClosingInserting(StringUtils.formatString(formatter.value)));
                    } else {
                        resolver = TagResolver.resolver(formatter.key, (arg, ctx) ->
                          Tag.preProcessParsed(formatter.value));
                    }
                    t.resolver(resolver);
                }

                t.resolver(TagResolver.resolver("small-caps", (args, ctx) -> new SmallCapsTag()));
            });

            StringUtils.setMiniMessage(builder.build());
        } catch (Exception e) {
            // Log do erro real
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize AdventureFormatters", e);
        }
    }
}
