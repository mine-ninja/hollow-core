package io.github.minehollow.kits.kit;

import io.github.minehollow.kits.kit.model.Cooldown;
import io.github.minehollow.kits.kit.model.Kit;
import io.github.minehollow.sdk.util.time.Time;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class KitService {

    private final KitRepository repository;
    private final Map<String, Kit> cachedKits = new ConcurrentHashMap<>();

    public KitService(KitRepository repository) {
        this.repository = repository;
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
            .thenCompose(res -> repository.deleteAllCooldownsForKit(id))
            .thenAccept(res -> cachedKits.remove(id));
    }

    public CompletableFuture<Kit> getKit(String id) {
        Kit cached = cachedKits.get(id);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return repository.findKitById(id).thenApply(kit -> {
            if (kit != null) cachedKits.put(kit.getId(), kit);
            return kit;
        });
    }

    public CompletableFuture<List<Kit>> getAllKits() {
        if (!cachedKits.isEmpty()) {
            return CompletableFuture.completedFuture(List.copyOf(cachedKits.values()));
        }
        return repository.findAllKits().thenApply(kits -> {
            kits.forEach(kit -> cachedKits.put(kit.getId(), kit));
            return kits;
        });
    }

    public CompletableFuture<Boolean> isOnCooldown(UUID playerId, String kitId) {
        return repository.findCooldownByPlayerAndKit(playerId, kitId)
            .thenApply(cooldown -> cooldown != null && cooldown.isActive());

    }

    public CompletableFuture<Void> setCooldown(UUID playerId, String kitId, long durationSeconds) {
        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000);
        Cooldown cooldown = new Cooldown(playerId, kitId, expiryTime);
        return repository.saveCooldown(cooldown).thenAccept(result -> {
        });
    }

    public CompletableFuture<Long> getRemainingTime(UUID playerId, String kitId) {
        return repository.findCooldownByPlayerAndKit(playerId, kitId)
            .thenApply(cooldown -> cooldown == null ? 0L : cooldown.getRemainingSeconds());
    }

    public CompletableFuture<Void> removeCooldown(UUID playerId, String kitId) {
        return repository.deleteCooldown(playerId, kitId).thenAccept(result -> {
        });
    }

    public String getFormattedRemainingTime(UUID playerId, String kitId) {
        long remainingMs = getRemainingTime(playerId, kitId).join() * 1000;
        return new Time(remainingMs).toString();
    }
}
