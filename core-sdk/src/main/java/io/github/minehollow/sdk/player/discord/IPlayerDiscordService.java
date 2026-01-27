package io.github.minehollow.sdk.player.discord;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IPlayerDiscordService {

    @Nullable PlayerDiscord loadFromRedis(@NotNull UUID playerId);

    void updateCaches(@NotNull PlayerDiscord playerDiscord);

    CompletableFuture<@Nullable PlayerDiscord> getPlayerDiscord(@NotNull UUID playerId);

    CompletableFuture<@Nullable PlayerDiscord> findByDiscordId(@NotNull String discordId);

    CompletableFuture<@NotNull PlayerDiscord> updatePlayerDiscord(@NotNull PlayerDiscord toUpdate);

    CompletableFuture<@NotNull PlayerDiscord> loadPlayerDiscord(@NotNull UUID playerId);

    void unloadPlayerDiscord(@NotNull UUID playerId);
}
