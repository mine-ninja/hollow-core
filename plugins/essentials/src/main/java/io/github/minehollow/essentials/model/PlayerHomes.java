package io.github.minehollow.essentials.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.codecs.pojo.annotations.BsonId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores all homes for a single player.
 * One document per player in MongoDB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerHomes {

    @BsonId
    private UUID playerId;

    private List<Home> homes;

    public static @NotNull PlayerHomes createEmpty(@NotNull UUID playerId) {
        return new PlayerHomes(playerId, new ArrayList<>());
    }

    public @Nullable Home getHome(@NotNull String name) {
        for (Home h : homes) {
            if (h.getName().equalsIgnoreCase(name)) return h;
        }
        return null;
    }

    public boolean addHome(@NotNull Home home) {
        // Replace if same name exists
        homes.removeIf(h -> h.getName().equalsIgnoreCase(home.getName()));
        return homes.add(home);
    }

    public boolean removeHome(@NotNull String name) {
        return homes.removeIf(h -> h.getName().equalsIgnoreCase(name));
    }

    public @NotNull List<String> getHomeNames() {
        return homes.stream().map(Home::getName).toList();
    }
}

