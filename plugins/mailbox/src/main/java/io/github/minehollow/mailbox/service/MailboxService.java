package io.github.minehollow.mailbox.service;

import io.github.minehollow.mailbox.MailboxPlugin;
import io.github.minehollow.mailbox.model.MailboxItem;
import io.github.minehollow.mailbox.model.PlayerMailboxData;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.sdk.util.data.MongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MailboxService implements Listener {
    private static final PredefinedSound RECEIVE_SOUND = new PredefinedSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F,
            1.2F);

    private final MailboxPlugin plugin;
    private final Map<UUID, PlayerMailboxData> cache = new ConcurrentHashMap<>();
    private final MongoRepository<UUID, PlayerMailboxData> repository;

    public MailboxService(@NotNull MailboxPlugin plugin) {
        this.plugin = plugin;
        this.repository = new MongoRepository<>(PlayerMailboxData.class, "_id", "player_mailbox");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Sends a box to a player's mailbox.
     * Works for both online and offline players.
     * If online, the player receives a notification.
     */
    public void sendToPlayer(@NotNull UUID playerId, @NotNull MailboxItem item) {
        var cached = cache.get(playerId);
        if (cached != null) {
            cached.getBoxes().add(item);
            save(cached);
        } else {
            Thread.startVirtualThread(() -> {
                var data = loadOrCreate(playerId);
                data.getBoxes().add(item);
                saveSync(data);
            });
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            StringUtils.send(player,
                    "<gradient:#FFD700:#FFA500>✉ Correio!</gradient> <gray>Você recebeu: <white>"
                            + item.getDescription());
            StringUtils.send(player,
                    "<gray>Use <yellow>/correio <gray>para resgatar.");
            RECEIVE_SOUND.play(player);
        }
    }

    /**
     * Loads data from cache or MongoDB. Caches the result.
     */
    @NotNull
    public PlayerMailboxData loadOrCreate(@NotNull UUID playerId) {
        var cached = cache.get(playerId);
        if (cached != null)
            return cached;

        var data = repository.findById(playerId);
        if (data == null) {
            data = PlayerMailboxData.createNew(playerId);
            repository.save(playerId, data);
        }
        cache.put(playerId, data);
        return data;
    }

    @Nullable
    public PlayerMailboxData getCachedData(@NotNull UUID playerId) {
        return cache.get(playerId);
    }

    /**
     * Claims a specific box: gives items to the player and removes it.
     * Overflow items are dropped on the ground.
     *
     * @return true if the box was found and claimed successfully
     */
    public boolean claimBox(@NotNull Player player, @NotNull String boxId) {
        var data = cache.get(player.getUniqueId());
        if (data == null)
            return false;

        var box = data.getBoxById(boxId);
        if (box == null)
            return false;

        ItemStack[] items = box.getItems();
        var leftover = player.getInventory().addItem(items);
        for (ItemStack overflow : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflow);
        }

        data.removeBox(boxId);
        save(data);
        return true;
    }

    /**
     * Claims all boxes in the player's mailbox.
     *
     * @return the number of boxes claimed
     */
    public int claimAll(@NotNull Player player) {
        var data = cache.get(player.getUniqueId());
        if (data == null || data.getBoxes().isEmpty())
            return 0;

        int claimed = 0;
        var boxes = new java.util.ArrayList<>(data.getBoxes());
        for (var box : boxes) {
            ItemStack[] items = box.getItems();
            var leftover = player.getInventory().addItem(items);
            for (ItemStack overflow : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), overflow);
            }
            data.removeBox(box.getId());
            claimed++;
        }

        save(data);
        return claimed;
    }

    public void save(@NotNull PlayerMailboxData data) {
        Thread.startVirtualThread(() -> saveSync(data));
    }

    public void saveSync(@NotNull PlayerMailboxData data) {
        var updated = repository.save(data.getPlayerId(), data);
        if (updated != null) {
            cache.put(updated.getPlayerId(), updated);
        }
    }

    /**
     * Removes the player's data from cache.
     * Called when the player quits, or after they close the menu.
     */
    public void unload(@NotNull UUID playerId) {
        var data = cache.remove(playerId);
        if (data != null) {
            repository.save(playerId, data);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void handleQuit(PlayerQuitEvent event) {
        var playerId = event.getPlayer().getUniqueId();
        if (cache.containsKey(playerId)) {
            Tasks.runAsync(() -> unload(playerId));
        }
    }
}
