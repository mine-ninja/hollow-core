package net.warcane.lugin.core.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Getter
@AllArgsConstructor
public enum PlayerGroup {

    MASTER("master", "§6[Master] ", '\uE003', "Master", NamedTextColor.GOLD, 11),
    MANAGER("manager", "§4[Gerente] ", '\uE004', "Gerente", NamedTextColor.DARK_RED, 11),
    ADMIN("admin", "§c[Admin] ", '\uE005', "Admin", NamedTextColor.RED, 10),
    MODERATOR("moderator", "§2[Moderador] ", '\uE006', "Moderador", NamedTextColor.DARK_GREEN, 9),
    HELPER("helper", "§e[Ajudante] ", '\uE007', "Ajudante", NamedTextColor.DARK_GREEN, 8),
    INFLUENCER("influencer", "§c[Influencer] ", ' ', "Influencer", NamedTextColor.LIGHT_PURPLE, 7), // TODO: esperar a tag
    SUPREME("supreme", "§4[Supremo] ", '\uE002', "Supremo", NamedTextColor.DARK_BLUE, 6),
    LEGENDARY("legendary", "§2[Lendário] ", '\uE001', "Lendário", NamedTextColor.BLUE, 5),
    HERO("hero", "§5[Heroi] ", ' ', "Hero", NamedTextColor.DARK_PURPLE, 4),
    CHAMPION("champion", "§3[Campeão] ", '\uE000', "Campeão", NamedTextColor.DARK_AQUA, 3),
    ALPHA("alpha", "§b[Alpha] ", '\uE00E', "Alpha", NamedTextColor.DARK_AQUA, 2),
    DEFAULT("default", "§7", ' ', "Membro", NamedTextColor.GRAY, 1); // TODO: esperar a tag


    public static final Map<String, PlayerGroup> BY_ID = Arrays.stream(values())
      .collect(toMap(PlayerGroup::getId, group -> group));

    public static final List<String> NAMES = Arrays.stream(values())
      .map(PlayerGroup::getId)
      .toList();

    public static final List<PlayerGroup> STAFF_GROUPS = Arrays.stream(values())
      .filter(PlayerGroup::isStaffGroup)
      .toList();

    public static final List<PlayerGroup> SPECIAL_GROUPS = Arrays.stream(values())
      .filter(PlayerGroup::isSpecialGroup)
      .toList();

    private static final int MAX_PRIORITY_VALUE = 90;


    private final String id;
    private final String prefix;
    private final char modernTag;
    private final String displayName;
    private final NamedTextColor namedTextColor;
    private final int powerLevel;


    public boolean isGreaterOrEqualTo(PlayerGroup other) {
        return this.powerLevel >= other.powerLevel;
    }

    public boolean isLowerThan(PlayerGroup other) {
        return this.powerLevel < other.powerLevel;
    }

    public char getPrefixColorCode() {
        if (prefix.isEmpty()) {
            return '7'; // Default color code for gray
        }
        return prefix.charAt(1);
    }

    public int getPriorityValue() {
        return MAX_PRIORITY_VALUE - this.powerLevel;
    }

    public String getColoredDisplayName() {
        return "§" + this.getPrefixColorCode() + this.displayName;
    }

    public boolean isStaffGroup(){
        return isGreaterOrEqualTo(HELPER);
    }

    public boolean isSpecialGroup(){
        return isGreaterOrEqualTo(INFLUENCER);
    }
}
