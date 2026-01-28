package io.github.minehollow.kits.kit;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import io.github.minehollow.kits.KitsPlugin;
import io.github.minehollow.kits.kit.model.Cooldown;
import io.github.minehollow.kits.kit.model.Kit;
import io.github.minehollow.sdk.database.MongoDbConnector;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class KitRepository implements Closeable {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final KitsPlugin plugin;

    private MongoClient mongoClient;
    private MongoCollection<Kit> kitsCollection;
    private MongoCollection<Cooldown> cooldownsCollection;

    public KitRepository(KitsPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            MongoDbConnector connector = MongoDbConnector.getInstance();

            this.kitsCollection = connector.getCollection("kits", Kit.class);
            this.cooldownsCollection = connector.getCollection("kit_cooldowns", Cooldown.class);

            this.cooldownsCollection.createIndex(Indexes.ascending("playerId"));
            this.cooldownsCollection.createIndex(
                Indexes.ascending("expiryTime"),
                new IndexOptions().expireAfter(0L, TimeUnit.SECONDS)
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize collections: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        this.executor.shutdown();
    }

    public CompletableFuture<UpdateResult> saveKit(Kit kit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return kitsCollection.replaceOne(
                    Filters.eq("_id", kit.getId()),
                    kit,
                    new ReplaceOptions().upsert(true)
                );
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save kit %s: %s".formatted(kit.getId(), e.getMessage()));
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<@Nullable Kit> findKitById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return kitsCollection.find(Filters.eq("_id", id)).first();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to find kit %s: %s".formatted(id, e.getMessage()));
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<List<Kit>> findAllKits() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return kitsCollection.find().into(new ArrayList<>());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to fetch all kits: %s".formatted(e.getMessage()));
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<DeleteResult> deleteKit(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return kitsCollection.deleteOne(Filters.eq("_id", id));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to delete kit %s: %s".formatted(id, e.getMessage()));
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<UpdateResult> saveCooldown(Cooldown cooldown) {
        return CompletableFuture.supplyAsync(() -> cooldownsCollection.replaceOne(
            Filters.eq("_id", cooldown.getId()),
            cooldown,
            new ReplaceOptions().upsert(true)
        ), executor);
    }

    public CompletableFuture<Cooldown> findCooldownByPlayerAndKit(UUID playerId, String kitId) {
        return CompletableFuture.supplyAsync(() -> {
            String id = "%s:%s".formatted(playerId.toString(), kitId);
            return cooldownsCollection.find(Filters.eq("_id", id)).first();
        }, executor);
    }

    public CompletableFuture<DeleteResult> deleteCooldown(UUID playerId, String kitId) {
        return CompletableFuture.supplyAsync(() -> {
            String id = "%s:%s".formatted(playerId.toString(), kitId);
            return cooldownsCollection.deleteOne(Filters.eq("_id", id));
        }, executor);
    }

    public CompletableFuture<DeleteResult> deleteAllCooldownsForPlayer(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> cooldownsCollection.deleteMany(Filters.eq("playerId", playerId)), executor);
    }

    public CompletableFuture<DeleteResult> deleteAllCooldownsForKit(String kitId) {
        return CompletableFuture.supplyAsync(() ->
            cooldownsCollection.deleteMany(Filters.eq("kitId", kitId)), executor);
    }
}
