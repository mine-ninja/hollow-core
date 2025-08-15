package net.warcane.lugin.core.server.type;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum GameType {

    BEDWARS_SOLO(0, "Bedwars Solo", ServerCategoryType.BEDWARS),
    BEDWARS_DOUBLES(1, "Bedwars Duplas", ServerCategoryType.BEDWARS),
    BEDWARS_QUARTER(2, "Bedwars Quarteto", ServerCategoryType.BEDWARS),

    FACTIONS_ICE(3, "Factions Ice", ServerCategoryType.FACTIONS),

    SKYWARS_SOLO(2, "Skywars Solo", ServerCategoryType.SKYWARS),
    SKYWARS_DOUBLES(2, "Skywars Duplas", ServerCategoryType.SKYWARS)

    ;

    public static final Int2ObjectMap<GameType> ID_MAP = Arrays.stream(values())
      .collect(Collectors.toMap(
        GameType::getId,
        gameType -> gameType,
        (v1, v2) -> v1,
        Int2ObjectOpenHashMap::new
      ));

    private final int id;
    private final String displayName;
    private final ServerCategoryType categoryType;

}
