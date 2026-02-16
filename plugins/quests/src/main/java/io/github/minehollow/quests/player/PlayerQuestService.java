package io.github.minehollow.quests.player;

import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.quests.QuestsPlugin;
import io.github.minehollow.sdk.util.data.MongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PlayerQuestService implements Listener {
    private final QuestsPlugin plugin;
    private final Map<UUID, PlayerQuestData> cache = new ConcurrentHashMap<>();
    private final MongoRepository<UUID, PlayerQuestData> repository;

    public PlayerQuestService(@NotNull QuestsPlugin plugin) {
        this.plugin = plugin;
        this.repository = new MongoRepository<>(PlayerQuestData.class, "_id", "player_quest_data");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Nullable
    public PlayerQuestData getCachedData(@NotNull UUID playerId) {
        return cache.get(playerId);
    }

    @NotNull
    public PlayerQuestData loadOrCreate(@NotNull UUID playerId, boolean cacheResult) {
        var data = repository.findById(playerId);
        if (data == null) {
            data = PlayerQuestData.createNew(playerId);
            repository.save(playerId, data);
        }

        if (cacheResult) {
            cache.put(playerId, data);
        }
        return data;
    }

    public void save(@NotNull PlayerQuestData data) {
        Thread.startVirtualThread(() -> {
            var updated = repository.save(data.getPlayerId(), data);
            if (updated != null) {
                cache.put(updated.getPlayerId(), updated);
            }
        });
    }

    public void saveSync(@NotNull PlayerQuestData data) {
        var updated = repository.save(data.getPlayerId(), data);
        if (updated != null) {
            cache.put(updated.getPlayerId(), updated);
        }
    }

    private void unload(@NotNull UUID playerId) {
        var data = cache.remove(playerId);
        if (data != null) {
            repository.save(playerId, data);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void handleJoin(PlayerJoinEvent event) {
        final var playerId = event.getPlayer().getUniqueId();
        Tasks.runAsync(() -> {
            var data = loadOrCreate(playerId, true);

            if (data.needsReset()) {
                data.resetForNewDay();
                plugin.getQuestManager().assignDailyQuests(data,
                        plugin.getQuestManager().getDailyQuestCount(event.getPlayer()));
                saveSync(data);
            } else if (data.getActiveQuests().size() < plugin.getQuestManager().getDailyQuestCount(event.getPlayer())
                    && !plugin.getQuestManager().getAllTemplates().isEmpty()) {

                plugin.getQuestManager().assignDailyQuests(data,
                        plugin.getQuestManager().getDailyQuestCount(event.getPlayer()));
                saveSync(data);
            }

            log.debug("Loaded quest data for player {}: {} active quests",
                    event.getPlayer().getName(), data.getActiveQuests().size());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void handleQuit(PlayerQuitEvent event) {
        Tasks.runAsync(() -> unload(event.getPlayer().getUniqueId()));
    }
}
