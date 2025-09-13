package net.warcane.lugin.core.minigames.util.bedwars;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public enum BedwarsMode {

    ALL("Geral", "bedwars_all"),
    SOLO("Solo", "bedwars_solo"),
    DUO("Dupla", "bedwars_duo"),
    TRIO("Trio", "bedwars_trio"),
    QUARTETO("Quarteto", "bedwars_quarteto");

    private final String name;
    private final String smallName;

    BedwarsMode(String name, String smallName) {
        this.name = name;
        this.smallName = smallName;
    }

    @Nullable
    public static BedwarsMode getByName(@NotNull String name) {
        for (BedwarsMode bedwarsMode : BedwarsMode.values())
            if (bedwarsMode.name.equalsIgnoreCase(name) || bedwarsMode.name().equalsIgnoreCase(name) || bedwarsMode.smallName.equalsIgnoreCase(name))
                return bedwarsMode;

        return null;
    }
}
