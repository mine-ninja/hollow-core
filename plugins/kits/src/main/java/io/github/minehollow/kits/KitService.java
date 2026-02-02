package io.github.minehollow.kits;

import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.kits.model.KitCategory;
import io.github.minehollow.kits.model.KitPlayerData;
import io.github.minehollow.minecraft.util.inventory.InventoryUtil;
import io.github.minehollow.sdk.util.time.Time;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class KitService {
    private final KitRepository repository;
    private final Map<String, KitCategory> cachedCategories = new ConcurrentHashMap<>();
    private final Map<String, Kit> cachedKits = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, KitPlayerData>> playerDataCache = new ConcurrentHashMap<>();

    public KitService(KitRepository repository) {
        this.repository = repository;
    }

    public void loadAllCategories() {
        repository.findAllCategories().thenAccept(categories -> {
            cachedCategories.clear();
            categories.forEach(c -> cachedCategories.put(c.getId(), c));
        }).join();
    }

    public List<KitCategory> getAllCategories() {
        return List.copyOf(cachedCategories.values());
    }

    public KitCategory getCategory(String id) {
        return cachedCategories.get(id);
    }

    public CompletableFuture<KitCategory> saveCategory(KitCategory category) {
        return repository.saveCategory(category).thenApply(r -> {
            cachedCategories.put(category.getId(), category);
            return category;
        });
    }

    public CompletableFuture<Void> deleteCategory(String id) {
        return repository.deleteCategory(id).thenAccept(r -> cachedCategories.remove(id));
    }

    public List<Kit> getKitsByCategory(String categoryId) {
        return cachedKits.values().stream()
            .filter(k -> categoryId.equals(k.getCategoryId()))
            .toList();
    }

    public Kit findKitByName(String name) {
        return cachedKits.values().stream()
            .filter(k -> k.getId().equalsIgnoreCase(name) || k.getDisplayName().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    public List<String> getKitNames() {
        return cachedKits.values().stream()
            .map(Kit::getId)
            .toList();
    }

    public void loadAllKits() {
        repository.findAllKits().thenAccept(kits -> {
            cachedKits.clear();
            kits.forEach(kit -> cachedKits.put(kit.getId(), kit));
        }).join();
    }

    public CompletableFuture<Kit> saveKit(Kit kit) {
        return repository.saveKit(kit).thenApply(result -> {
            cachedKits.put(kit.getId(), kit);
            return kit;
        });
    }

    public CompletableFuture<Void> deleteKit(String id) {
        return repository.deleteKit(id)
            .thenCompose(res -> repository.deleteAllPlayerDataForKit(id))
            .thenAccept(res -> cachedKits.remove(id));
    }

    public CompletableFuture<Kit> getKit(String id) {
        Kit cached = cachedKits.get(id);
        if (cached != null)
            return CompletableFuture.completedFuture(cached);

        return repository.findKitById(id).thenApply(kit -> {
            if (kit != null)
                cachedKits.put(kit.getId(), kit);
            return kit;
        });
    }

    public CompletableFuture<List<Kit>> getAllKits() {
        if (!cachedKits.isEmpty()) {
            return CompletableFuture.completedFuture(List.copyOf(cachedKits.values()));
        }
        return repository.findAllKits().thenApply(kits -> {
            kits.forEach(kit -> cachedKits.put(kit.getId(), kit));
            return List.copyOf(kits);
        });
    }

    public CompletableFuture<Void> setCooldown(UUID playerId, String kitId, long durationSeconds) {
        return repository.findPlayerData(playerId, kitId).thenCompose(existing -> {
            KitPlayerData data;
            if (existing != null) {
                existing.recordClaim(durationSeconds);
                data = existing;
            } else {
                data = new KitPlayerData(playerId, kitId, durationSeconds);
            }
            return repository.savePlayerData(data).thenAccept(result -> {
                Map<String, KitPlayerData> playerCache = playerDataCache.get(playerId);
                if (playerCache != null) {
                    playerCache.put(kitId, data);
                }
            });
        });
    }

    public CompletableFuture<Long> getRemainingTime(UUID playerId, String kitId) {
        return repository.findPlayerData(playerId, kitId)
            .thenApply(data -> data == null ? 0L : data.getRemainingSeconds());
    }

    public CompletableFuture<Void> removeCooldown(UUID playerId, String kitId) {
        return repository.deletePlayerData(playerId, kitId).thenAccept(result -> {
        });
    }

    public CompletableFuture<Map<String, KitPlayerData>> loadPlayerData(UUID playerId) {
        return repository.findAllPlayerDataByPlayer(playerId).thenApply(dataList -> {
            Map<String, KitPlayerData> map = new HashMap<>();
            for (KitPlayerData d : dataList) {
                map.put(d.getKitId(), d);
            }
            playerDataCache.put(playerId, map);
            return map;
        });
    }

    public long getCachedRemainingTime(UUID playerId, String kitId) {
        KitPlayerData d = playerDataCache.getOrDefault(playerId, Map.of()).get(kitId);
        return d != null ? d.getRemainingSeconds() : 0L;
    }

    public void evictPlayerCache(UUID playerId) {
        playerDataCache.remove(playerId);
    }

    public boolean hasPermission(Player player, Kit kit) {
        return player.hasPermission(kit.getPermission()) || player.hasPermission("kits.admin");
    }

    public CompletableFuture<ClaimResult> claimKit(Player player, Kit kit) {
        if (kit == null) {
            return CompletableFuture.completedFuture(new ClaimResult(KitResult.NOT_FOUND, 0));
        }

        UUID playerId = player.getUniqueId();
        String kitId = kit.getId();

        if (!hasPermission(player, kit)) {
            return CompletableFuture.completedFuture(new ClaimResult(KitResult.NO_PERMISSION, 0));
        }

        ItemStack[] items = kit.getItems().toArray(ItemStack[]::new);

        return getRemainingTime(playerId, kitId).thenCompose(remainingSeconds -> {
            if (remainingSeconds > 0) {
                return CompletableFuture.completedFuture(new ClaimResult(KitResult.ON_COOLDOWN, remainingSeconds));
            }

            if (!InventoryUtil.fits(player.getInventory(), items)) {
                return CompletableFuture.completedFuture(new ClaimResult(KitResult.NO_SPACE, 0));
            }

            InventoryUtil.give(player, false, items);

            return setCooldown(playerId, kitId, kit.getCooldown())
                .thenApply(v -> new ClaimResult(KitResult.SUCCESS, 0));
        });
    }

    public KitResult giveKit(Player player, Kit kit) {
        if (kit == null) {
            return KitResult.NOT_FOUND;
        }

        ItemStack[] items = kit.getItems().toArray(ItemStack[]::new);

        if (!InventoryUtil.fits(player.getInventory(), items)) {
            return KitResult.NO_SPACE;
        }

        InventoryUtil.give(player, false, items);

        return KitResult.SUCCESS;
    }

    public record ClaimResult(KitResult status, long remainingSeconds) {
        public String getFormattedRemainingTime() {
            return new Time(remainingSeconds * 1000).toString();
        }
    }

    public enum KitResult {
        SUCCESS,
        NOT_FOUND,
        NO_PERMISSION,
        ON_COOLDOWN,
        NO_SPACE
    }
}
