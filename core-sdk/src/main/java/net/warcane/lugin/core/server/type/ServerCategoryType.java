package net.warcane.lugin.core.server.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@Getter
@AllArgsConstructor
public enum ServerCategoryType {


    LOGIN("Servidor de Login"),
    LOBBY("Lobby"),
    BEDWARS("BedWars"),
    MINA("Mina"),
    FACTIONS("Factions");

    public static final Map<String, ServerCategoryType> entries = Arrays.stream(values())
      .collect(toMap(ServerCategoryType::name, Function.identity()));


    @NotNull
    public static ServerCategoryType fromName(@NotNull String name) {
        return Objects.requireNonNull(entries.get(name.toUpperCase()),
          "ServerCategoryType not found for name: " + name);
    }

    private final String displayName;

}
