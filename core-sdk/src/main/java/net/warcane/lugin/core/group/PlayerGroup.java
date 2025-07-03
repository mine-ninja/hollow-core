package net.warcane.lugin.core.group;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PlayerGroup {
    MASTER("master", "§6[Master]", 10),
    MANAGER("manager", "§4[Gerente] ", 9),
    ADMIN("admin", "§c[Admin]", 8),
    MODERATOR("moderator", "§2[Moderador]", 7),
    HELPER("helper", "§e[Ajudante]", 6),
    INFLUENCER("influencer", "§c[Influencer]", 5),
    YOUTUBER("youtuber", "§b[Youtuber]", 4),
    VIP_PLUS("vip_plus", "§b[VIP+]", 3),
    VIP("vip", "§a[VIP]", 2),
    MEMBER("member", "§7", 1);

    private final String id;
    private final String prefix;
    private int powerLevel;
}
