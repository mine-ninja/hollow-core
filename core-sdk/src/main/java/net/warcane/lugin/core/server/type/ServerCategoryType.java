package net.warcane.lugin.core.server.type;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import java.util.Arrays;
import java.util.List;
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
    SKYWARS("SkyWars"),
    MINA("Mina", List.of(ServerSubCategoryType.MINA_1, ServerSubCategoryType.MINA_2, ServerSubCategoryType.MINA_3)),
    FACTIONS("Factions");
    
    private final String displayName;
    private final List<ServerSubCategoryType> subCategories;
    
    ServerCategoryType(String displayName) {
        this(displayName, List.of());
    }
    
    public static final Map<String, ServerCategoryType> entries = Arrays.stream(values())
        .collect(toMap(ServerCategoryType::name, Function.identity()));
    
    @NotNull
    public static ServerCategoryType fromName(@NotNull String name) {
        return Objects.requireNonNull(entries.get(name.toUpperCase()), "ServerCategoryType not found for name: " + name);
    }
}
