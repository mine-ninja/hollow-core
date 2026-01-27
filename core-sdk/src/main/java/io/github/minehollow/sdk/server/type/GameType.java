package io.github.minehollow.sdk.server.type;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum GameType {

    BEDWARS(0, "Bed Wars", ServerCategoryType.BEDWARS),
    BEDWARS_SOLO(1, "Bed Wars Solo", ServerCategoryType.BEDWARS),
    BEDWARS_DOUBLES(2, "Bed Wars Duplas", ServerCategoryType.BEDWARS),
    BEDWARS_QUARTER(3, "Bed Wars Quarteto", ServerCategoryType.BEDWARS),
    FACTIONS(4, "Factions", ServerCategoryType.FACTIONS),
    SKYWARS(5, "Sky Wars", ServerCategoryType.SKYWARS),
    SKYWARS_SOLO(6, "Sky Wars Solo", ServerCategoryType.SKYWARS),
    SKYWARS_DOUBLES(7, "Sky Wars Duplas", ServerCategoryType.SKYWARS),
    FACTIONS_FIRE(8, "Factions Fire", ServerCategoryType.FACTIONS_FIRE),
    SMP(9, "SMP", ServerCategoryType.SMP);

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
