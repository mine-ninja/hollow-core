package net.warcane.lugin.core.group;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Getter
@AllArgsConstructor
public enum PlayerGroup {

    MASTER("master", "§6[Master] ", 11),
    MANAGER("manager", "§4[Gerente] ", 11),
    ADMIN("admin", "§c[Admin] ", 10),
    MODERATOR("moderator", "§2[Moderador] ", 9),
    HELPER("helper", "§e[Ajudante] ", 8),
    INFLUENCER("influencer", "§c[Influencer] ", 7),
    SUPREME("supreme", "§4[Supremo] ", 6),
    LEGENDARY("legendary", "§2[Lendário] ", 5),
    HERO("hero", "§5[Heroi] ", 4),
    CHAMPION("champion", "§3[Campeão] ", 3),
    DEFAULT("member", "§7", 1);


    public static final Map<String, PlayerGroup> BY_ID = Arrays.stream(values())
      .collect(toMap(PlayerGroup::getId, group -> group));

    public static final List<String> NAMES = Arrays.stream(values())
      .map(PlayerGroup::getId)
      .toList();

    private static final int MAX_PRIORITY_VALUE = 90;


    private final String id;
    private final String prefix;
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
}
