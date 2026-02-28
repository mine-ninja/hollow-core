package io.github.minehollow.essentials.repository;

import io.github.minehollow.essentials.model.PlayerHomes;
import io.github.minehollow.sdk.util.data.MongoRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Thin wrapper around {@link MongoRepository} for PlayerHomes documents.
 * All operations are blocking — call from virtual threads.
 */
public class HomeRepository {

    private final MongoRepository<UUID, PlayerHomes> repository;

    public HomeRepository() {
        this.repository = new MongoRepository<>(PlayerHomes.class, "_id", "player_homes");
    }

    public @Nullable PlayerHomes findByPlayer(@NotNull UUID playerId) {
        return repository.findById(playerId);
    }

    public @NotNull PlayerHomes save(@NotNull PlayerHomes playerHomes) {
        return repository.save(playerHomes.getPlayerId(), playerHomes);
    }
}

