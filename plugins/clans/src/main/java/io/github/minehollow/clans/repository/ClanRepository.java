package io.github.minehollow.clans.repository;

import io.github.minehollow.clans.model.Clan;
import io.github.minehollow.sdk.util.data.MongoRepository;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Thin wrapper around {@link MongoRepository} for Clan documents.
 * All operations are blocking — call from virtual threads.
 */
public class ClanRepository {

    private final MongoRepository<String, Clan> repository;

    public ClanRepository() {
        this.repository = new MongoRepository<>(Clan.class, "_id", "clans");
    }

    public @Nullable Clan findByTag(@NotNull String tag) {
        return repository.findById(tag.toUpperCase());
    }

    public @Nullable Clan findByMember(@NotNull UUID playerId) {
        return repository.findFirstFromProperty("members.uuid", playerId);
    }

    public @Nullable Clan findByOwner(@NotNull UUID ownerId) {
        return repository.findFirstFromProperty("ownerId", ownerId);
    }

    public @NotNull Clan save(@NotNull Clan clan) {
        return repository.save(clan.getTag(), clan);
    }

    public @Nullable Clan delete(@NotNull String tag) {
        return repository.deleteById(tag.toUpperCase());
    }

    public @NotNull List<Clan> findAll() {
        return repository.queryAll();
    }
}

