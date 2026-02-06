package io.github.minehollow.kits;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.kits.model.KitCategory;
import io.github.minehollow.kits.model.KitPlayerData;
import io.github.minehollow.sdk.database.MongoDbConnector;

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

    private MongoCollection<Kit> kitsCollection;
    private MongoCollection<KitCategory> categoriesCollection;
    private MongoCollection<KitPlayerData> playerDataCollection;

    public KitRepository(KitsPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            MongoDbConnector connector = MongoDbConnector.getInstance();

            this.kitsCollection = connector.getCollection("kits", Kit.class);
            this.categoriesCollection = connector.getCollection("kit_categories", KitCategory.class);
            this.playerDataCollection = connector.getCollection("kit_player_data", KitPlayerData.class);

            this.playerDataCollection.createIndex(Indexes.ascending("playerId"));
            this.playerDataCollection.createIndex(Indexes.ascending("kitId"));
            this.playerDataCollection.createIndex(
                Indexes.ascending("cooldownExpiry"),
                new IndexOptions().expireAfter(0L, TimeUnit.SECONDS));
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
                    new ReplaceOptions().upsert(true));
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save kit %s: %s".formatted(kit.getId(), e.getMessage()));
                throw new RuntimeException(e);
            }
        }, executor);
    }

    public CompletableFuture<Kit> findKitById(String id) {
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

    public CompletableFuture<UpdateResult> savePlayerData(KitPlayerData data) {
        return CompletableFuture.supplyAsync(() -> playerDataCollection.replaceOne(
            Filters.eq("_id", data.getId()),
            data,
            new ReplaceOptions().upsert(true)), executor);
    }

    public CompletableFuture<KitPlayerData> findPlayerData(UUID playerId, String kitId) {
        return CompletableFuture.supplyAsync(() -> {
            String id = "%s:%s".formatted(playerId.toString(), kitId);
            return playerDataCollection.find(Filters.eq("_id", id)).first();
        }, executor);
    }

    public CompletableFuture<List<KitPlayerData>> findAllPlayerDataByPlayer(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> playerDataCollection.find(Filters.eq("playerId", playerId))
            .into(new ArrayList<>()), executor);
    }

    public CompletableFuture<DeleteResult> deletePlayerData(UUID playerId, String kitId) {
        return CompletableFuture.supplyAsync(() -> {
            String id = "%s:%s".formatted(playerId.toString(), kitId);
            return playerDataCollection.deleteOne(Filters.eq("_id", id));
        }, executor);
    }

    public CompletableFuture<DeleteResult> deleteAllPlayerDataForKit(String kitId) {
        return CompletableFuture.supplyAsync(() -> playerDataCollection.deleteMany(Filters.eq("kitId", kitId)),
            executor);
    }

    public CompletableFuture<UpdateResult> saveCategory(KitCategory category) {
        return CompletableFuture.supplyAsync(() -> categoriesCollection.replaceOne(
            Filters.eq("_id", category.getId()),
            category,
            new ReplaceOptions().upsert(true)), executor);
    }

    public CompletableFuture<List<KitCategory>> findAllCategories() {
        return CompletableFuture.supplyAsync(() -> categoriesCollection.find().into(new ArrayList<>()), executor);
    }

    public CompletableFuture<DeleteResult> deleteCategory(String id) {
        return CompletableFuture.supplyAsync(() -> categoriesCollection.deleteOne(Filters.eq("_id", id)), executor);
    }
}
