package net.warcane.lugin.core.server.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServerCategoryType {

    LOBBY(0, "Lobby"),
    BEDWARS(1, "BedWars"),
    FACTIONS(2, "Factions"),
    ;

    private final int id;
    private final String displayName;
}
