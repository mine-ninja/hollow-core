package io.github.minehollow.skills.player;

import io.github.minehollow.minecraft.util.exception.MainThreadViolationError;
import io.github.minehollow.sdk.util.data.MongoRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSkillsProgressService implements Listener {


    private final Map<UUID, PlayerSkillsProgress> playerSkillsProgressMap;
    private final MongoRepository<UUID, PlayerSkillsProgress> repository;

    public PlayerSkillsProgressService() {
        this.playerSkillsProgressMap = new ConcurrentHashMap<>();
        this.repository = new MongoRepository<>(PlayerSkillsProgress.class);
    }

    public PlayerSkillsProgress getCachedPlayerProgress(@NotNull UUID playerId) {
        return playerSkillsProgressMap.get(playerId);
    }

    public PlayerSkillsProgress fetchPlayerProgress(
      @NotNull UUID playerId,
      boolean createIfAbsent,
      boolean cacheResult
    ) {
        MainThreadViolationError.throwIfApplicable();

        var result = repository.findById(playerId);
        if (result == null) {
            if (!createIfAbsent) {
                return null;
            }

            result = new PlayerSkillsProgress(playerId, new HashMap<>());
            this.saveProgress(result, false);
        }

        if (cacheResult) {
            playerSkillsProgressMap.put(playerId, result);
        }

        return result;
    }

    public void unloadPlayerProgress(@NotNull UUID playerId) {
        var progress = playerSkillsProgressMap.remove(playerId);
        if (progress != null) {
            saveProgress(progress, false);
        }
    }


    public void saveProgress(@NotNull PlayerSkillsProgress progress) {
        saveProgress(progress, true);
    }

    public void saveProgress(PlayerSkillsProgress progress, boolean cacheUpdatedResult) {
        Thread.startVirtualThread(() -> {
            final var updated = repository.save(progress.getPlayerId(), progress);
            if (updated == null) {
                throw new IllegalStateException("Failed to save PlayerSkillsProgress for playerId: " + progress.getPlayerId());
            }

            if (!cacheUpdatedResult) {
                return;
            }

            playerSkillsProgressMap.put(updated.getPlayerId(), updated);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void handleJoin(PlayerJoinEvent event) {
        fetchPlayerProgress(event.getPlayer().getUniqueId(), true, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void saveOnQuit(PlayerJoinEvent event) {
        unloadPlayerProgress(event.getPlayer().getUniqueId());
    }
}
