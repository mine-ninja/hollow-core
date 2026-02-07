package io.github.minehollow.ranks.progress;

import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.sdk.util.data.MongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PlayerRankProgressService implements Listener {

    private final RanksPlugin plugin;
    private final Map<UUID, PlayerRankProgress> localCache = new ConcurrentHashMap<>(Bukkit.getMaxPlayers());
    private final MongoRepository<UUID, PlayerRankProgress> repository = new MongoRepository<>(PlayerRankProgress.class);

    public PlayerRankProgressService(@NotNull RanksPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void handleJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();

        Thread.startVirtualThread(() -> {
            var playerProgress = loadProgressOrCreate(player.getUniqueId(), true);
            log.debug("Loaded progress for player {}: {}", player.getName(), playerProgress);
        });
    }

    @EventHandler
    void handleQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        invalidate(player.getUniqueId());
    }

    public PlayerRankProgress getCachedProgress(UUID playerId) {
        return localCache.get(playerId);
    }

    public PlayerRankProgress loadProgressOrCreate(
      @NotNull UUID playerId,
      boolean cacheResult
    ) {
        var progress = loadProgress(playerId, cacheResult);
        if (progress != null) {
            return progress;
        }

        progress = PlayerRankProgress.createNewProgress(playerId);
        repository.save(playerId, progress);
        if (cacheResult) {
            localCache.put(playerId, progress);
        }

        return progress;
    }


    public PlayerRankProgress loadProgress(
      @NotNull UUID playerId,
      boolean cacheResult
    ) {

        final var fromDb = repository.findById(playerId);
        if (fromDb != null && cacheResult) {
            localCache.put(playerId, fromDb);
        }

        return fromDb;
    }

    public void invalidate(UUID playerId) {
        Thread.startVirtualThread(() -> {
            final var progress = localCache.remove(playerId);
            if (progress != null) {
                repository.save(playerId, progress);
            }
        });
    }

    public void updatePlayerProgress(PlayerRankProgress progress) {
        updatePlayerProgress(progress, true);
    }

    public void updatePlayerProgress(PlayerRankProgress progress, boolean cacheUpdated) {
        Thread.startVirtualThread(() -> {
            final var updated = repository.save(progress.getPlayerId(), progress);
            if (cacheUpdated) {
                localCache.put(progress.getPlayerId(), updated);
            }
        });
    }
}
