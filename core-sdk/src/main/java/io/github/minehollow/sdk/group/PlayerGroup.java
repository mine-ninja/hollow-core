package io.github.minehollow.sdk.group;

import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;

@Getter
public enum PlayerGroup {
    // STAFF
    GM("gamemaster", "<white><bold>GM </bold><white>", '\uE004', "Game Master", NamedTextColor.DARK_PURPLE, 11),
    ADMIN("admin", "<dark_red><bold>ADM </bold><dark_red>", '\uE005', "Admin", NamedTextColor.RED, 10),
    MODERATOR("moderador", "<dark_green><bold>MOD </bold><dark_green>", '\uE006', "Moderador", NamedTextColor.DARK_GREEN, 9),
    HELPER("help", "<yellow><bold>AJD </bold><yellow>", '\uE007', "Ajudante", NamedTextColor.YELLOW, 8),

    // SPECIAL
    INFLUENCER("streamer", "<blue><bold>STREAMER </bold><blue>", '\uE00B', "Streamer", NamedTextColor.BLUE, 7),

    // VIPS
    HOLLOW("hollow", "<gradient:#E0AAFF:#9D4EDD><bold>HOLLOW </bold></gradient><gradient:#E0AAFF:#9D4EDD>", '\uE00D', "Hollow", NamedTextColor.LIGHT_PURPLE, 6),
    DEMON("demon", "<dark_red><bold>DEMON </bold><dark_red>", '\uE00C', "Demon", NamedTextColor.DARK_RED, 5),
    HERO("hero", "<red><bold>HERO </bold><red>", ' ', "Hero", NamedTextColor.RED, 4),
    CHAMPION("campeao", "<gold><bold>CHAMPION </bold><gold>", ' ', "Champion", NamedTextColor.GOLD, 3),

    // DEFAULT
    DEFAULT("default", "<gray>", ' ', "Membro", NamedTextColor.GRAY, 1);

    public static final Map<String, PlayerGroup> BY_ID = Arrays.stream(values()).collect(toMap(PlayerGroup::getId, Function.identity()));
    public static final List<String> NAMES = Arrays.stream(values()).map(PlayerGroup::getId).toList();
    public static final List<PlayerGroup> entries = List.of(values());
    public static final List<PlayerGroup> STAFF_GROUPS = Arrays.stream(values()).filter(PlayerGroup::isStaffGroup).toList();
    public static final List<PlayerGroup> SPECIAL_GROUPS = Arrays.stream(values()).filter(PlayerGroup::isSpecialGroup).toList();

    private static final int MAX_PRIORITY_VALUE = 90;

    // Patterns compilados uma única vez
    private static final Pattern NAMED_COLOR_PATTERN = Pattern.compile("<(dark_red|dark_green|dark_blue|dark_aqua|dark_purple|dark_gray|gold|gray|blue|green|aqua|red|light_purple|yellow|white|black)>");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[0-9A-Fa-f]{6}):(#[0-9A-Fa-f]{6})>");
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([0-9A-Fa-f]{6})>");

    // Cache das cores já calculadas
    private static final Map<PlayerGroup, TextColor> LAST_COLOR_CACHE = new EnumMap<>(PlayerGroup.class);

    private final String id;
    private final String prefix;
    private final char modernTag;
    private final String displayName;
    private final NamedTextColor namedTextColor;
    private final int powerLevel;
    private final String teamName;

    PlayerGroup(String id, String prefix, char modernTag, String displayName, NamedTextColor namedTextColor, int powerLevel) {
        this.id = id;
        this.prefix = prefix;
        this.modernTag = modernTag;
        this.displayName = displayName;
        this.namedTextColor = namedTextColor;
        this.powerLevel = powerLevel;
        this.teamName = "HL-" + id + "-" + (MAX_PRIORITY_VALUE - powerLevel);
    }

    @Nullable
    public static PlayerGroup fromId(@NotNull String id) {
        return BY_ID.get(id.toLowerCase());
    }

    public boolean isGreaterOrEqualTo(PlayerGroup other) {
        return this.powerLevel >= other.powerLevel;
    }

    public boolean isLowerThan(PlayerGroup other) {
        return this.powerLevel < other.powerLevel;
    }

    public String formatPlayerNameString(@NotNull String playerName) {
        return this.getPrefix() + playerName;
    }

    /**
     * Retorna a última cor usada na tag MiniMessage do grupo
     * Útil para manter consistência de cor em formatações
     */
    public TextColor getLastTagColor() {
        return LAST_COLOR_CACHE.computeIfAbsent(this, PlayerGroup::calculateLastColor);
    }

    private static TextColor calculateLastColor(PlayerGroup group) {
        String tag = group.prefix;
        TextColor lastColor = null;
        int lastIndex = -1;

        // Procura cores nomeadas
        Matcher namedMatcher = NAMED_COLOR_PATTERN.matcher(tag);
        while (namedMatcher.find()) {
            if (namedMatcher.start() > lastIndex) {
                lastIndex = namedMatcher.start();
                lastColor = NamedTextColor.NAMES.value(namedMatcher.group(1));
            }
        }

        // Procura gradientes (pega a cor final)
        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(tag);
        while (gradientMatcher.find()) {
            if (gradientMatcher.start() > lastIndex) {
                lastIndex = gradientMatcher.start();
                lastColor = TextColor.fromHexString(gradientMatcher.group(2));
            }
        }

        // Procura cores hex
        Matcher hexMatcher = HEX_PATTERN.matcher(tag);
        while (hexMatcher.find()) {
            if (hexMatcher.start() > lastIndex) {
                lastIndex = hexMatcher.start();
                lastColor = TextColor.fromHexString("#" + hexMatcher.group(1));
            }
        }

        return lastColor != null ? lastColor : group.namedTextColor;
    }

    /**
     * Retorna o código de cor legacy para compatibilidade reversa
     * Extrai da NamedTextColor associada
     */
    public char getPrefixColorCode() {
        if (namedTextColor.equals(NamedTextColor.DARK_RED)) {
            return '4';
        } else if (namedTextColor.equals(NamedTextColor.RED)) {
            return 'c';
        } else if (namedTextColor.equals(NamedTextColor.GOLD)) {
            return '6';
        } else if (namedTextColor.equals(NamedTextColor.YELLOW)) {
            return 'e';
        } else if (namedTextColor.equals(NamedTextColor.DARK_GREEN)) {
            return '2';
        } else if (namedTextColor.equals(NamedTextColor.GREEN)) {
            return 'a';
        } else if (namedTextColor.equals(NamedTextColor.AQUA)) {
            return 'b';
        } else if (namedTextColor.equals(NamedTextColor.DARK_AQUA)) {
            return '3';
        } else if (namedTextColor.equals(NamedTextColor.DARK_BLUE)) {
            return '1';
        } else if (namedTextColor.equals(NamedTextColor.BLUE)) {
            return '9';
        } else if (namedTextColor.equals(NamedTextColor.LIGHT_PURPLE)) {
            return 'd';
        } else if (namedTextColor.equals(NamedTextColor.DARK_PURPLE)) {
            return '5';
        } else if (namedTextColor.equals(NamedTextColor.WHITE)) {
            return 'f';
        } else if (namedTextColor.equals(NamedTextColor.GRAY)) {
            return '7';
        } else if (namedTextColor.equals(NamedTextColor.DARK_GRAY)) {
            return '8';
        } else if (namedTextColor.equals(NamedTextColor.BLACK)) {
            return '0';
        }
        throw new IllegalArgumentException();
    }

    public int getPriorityValue() {
        return MAX_PRIORITY_VALUE - this.powerLevel;
    }

    public String getColoredDisplayName() {
        return "§" + this.getPrefixColorCode() + this.displayName;
    }

    public boolean isStaffGroup() {
        return isGreaterOrEqualTo(HELPER);
    }

    public boolean isSpecialGroup() {
        return isGreaterOrEqualTo(INFLUENCER);
    }
}