package net.warcane.lugin.core.minecraft.gamerule.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minecraft.gamerule.CustomGameRule;
import net.warcane.lugin.core.minecraft.gamerule.GameRuleRegistry;
import net.warcane.lugin.core.network.NetworkClient;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.gamerule.GameRuleUpdatePacket;
import net.warcane.lugin.core.util.property.Property;
import org.bson.Document;
import redis.clients.jedis.params.SetParams;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Handles persistence and cross-server synchronization of custom game rules.
 * Uses MongoDB for persistent storage and Redis for caching.
 * Cross-server sync is handled via NetworkClient using the OPERATION channel.
 *
 * CACHE STRATEGY:
 * - World-specific rules: Each server has isolated cache (gamerule:{serverId}:{world})
 * - Global rules: Shared cache across servers (gamerule:shared:_global_)
 *
 * PERFORMANCE: Uses Lua scripts for atomic Redis operations.
 */
@Slf4j
public class GameRuleStorage {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String REDIS_KEY_PREFIX = "gamerule:";
    private static final String SHARED_PREFIX = "shared";
    private static final String GLOBAL_SCOPE = "_global_";
    private static final String MONGO_COLLECTION = "game_rules";
    
    // Lua script for atomic update of a single rule in the JSON cache
    // KEYS[1] = redis key, ARGV[1] = ruleName, ARGV[2] = value (JSON), ARGV[3] = TTL
    private static final String LUA_UPDATE_RULE =
        "local cached = redis.call('GET', KEYS[1]) " +
        "if cached then " +
        "  local rules = cjson.decode(cached) " +
        "  rules[ARGV[1]] = cjson.decode(ARGV[2]) " +
        "  local updated = cjson.encode(rules) " +
        "  redis.call('SETEX', KEYS[1], ARGV[3], updated) " +
        "  return 1 " +
        "else " +
        "  return 0 " +
        "end";
    
    // Lua script for atomic removal of a single rule from the JSON cache
    // KEYS[1] = redis key, ARGV[1] = ruleName, ARGV[2] = TTL
    private static final String LUA_REMOVE_RULE =
        "local cached = redis.call('GET', KEYS[1]) " +
        "if cached then " +
        "  local rules = cjson.decode(cached) " +
        "  rules[ARGV[1]] = nil " +
        "  local updated = cjson.encode(rules) " +
        "  redis.call('SETEX', KEYS[1], ARGV[2], updated) " +
        "  return 1 " +
        "else " +
        "  return 0 " +
        "end";
    
    private final MongoCollection<Document> collection;
    private final RedisConnector redisConnector;
    private final NetworkClient networkClient;
    private final ExecutorService executorService;
    private final String serverId;
    
    public GameRuleStorage(@NotNull NetworkClient networkClient, @NotNull ExecutorService executorService) {
        this.redisConnector = RedisConnector.getInstance();
        this.collection = MongoDbConnector.getInstance().getCollection(MONGO_COLLECTION, Document.class);
        this.networkClient = networkClient;
        this.executorService = executorService;
        this.serverId = Property.get("SERVER_ID", "default");
    }
    
    /**
     * Loads all game rules for a specific world from MongoDB.
     * For global rules, uses GLOBAL_SCOPE instead of world name.
     *
     * @param worldName the world name (or null for global rules)
     * @return CompletableFuture with map of rule names to values
     */
    public CompletableFuture<Map<String, Object>> loadGameRules(@Nullable String worldName) {
        final String scope = worldName != null ? worldName : GLOBAL_SCOPE;
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First try Redis cache with server-specific key
                final String redisKey = buildRedisKey(scope);
                final String cached = redisConnector.supplyFromJedis(jedis -> jedis.get(redisKey));
                if (cached != null && !cached.isEmpty()) {
                    log.debug("Loaded game rules for scope {} from Redis cache", scope);
                    return deserializeGameRules(cached);
                }
                // If not in cache, load from MongoDB
                final Document doc = collection.find(Filters.eq("_id", scope)).first();
                if (doc == null) {
                    log.debug("No game rules found for scope {} in database", scope);
                    return new HashMap<>();
                }
                @SuppressWarnings("unchecked") final Map<String, Object> rules = (Map<String, Object>) doc.get("rules");
                final Map<String, Object> parsedRules = new HashMap<>();
                if (rules != null) {
                    // Parse values back to their correct types
                    for (Map.Entry<String, Object> entry : rules.entrySet()) {
                        final CustomGameRule<?> gameRule = GameRuleRegistry.getGameRule(entry.getKey());
                        if (gameRule != null) {
                            parsedRules.put(entry.getKey(), parseValue(gameRule, entry.getValue()));
                        }
                    }
                    // Cache in Redis for fast access
                    final String serialized = serializeGameRules(parsedRules);
                    redisConnector.useJedis(jedis -> jedis.set(redisKey, serialized, SetParams.setParams().ex(3600)));
                }
                log.debug("Loaded {} game rules for scope {} from MongoDB", parsedRules.size(), scope);
                return parsedRules;
            } catch (Exception e) {
                log.error("Failed to load game rules for scope {}: {}", scope, e.getMessage(), e);
                return new HashMap<>();
            }
        }, executorService);
    }
    
    /**
     * Saves a game rule value to both MongoDB and Redis, then publishes update to other servers.
     * OPTIMIZED: Uses Lua script for atomic Redis update (single round-trip).
     *
     * @param worldName the world name (or null for global rules)
     * @param ruleName  the rule name
     * @param value     the value to save
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Void> saveGameRule(@Nullable String worldName, @NotNull String ruleName, @NotNull Object value) {
        final String scope = worldName != null ? worldName : GLOBAL_SCOPE;
        return CompletableFuture.runAsync(() -> {
            try {
                final Document filter = new Document("_id", scope);
                final Document update = new Document("$set", new Document("rules." + ruleName, value).append("updatedAt", System.currentTimeMillis()));
                collection.updateOne(filter, update, new UpdateOptions().upsert(true));
                
                final String redisKey = buildRedisKey(scope);
                final String valueJson = GSON.toJson(value);
                redisConnector.useJedis(jedis -> {
                    Object result = jedis.eval(LUA_UPDATE_RULE, 1, redisKey, ruleName, valueJson, "3600");
                    if (result != null && ((Long) result) == 0L) {
                        log.debug("Cache for scope {} doesn't exist, will be loaded on next read", scope);
                    }
                });
                
                networkClient.sendNetworkPacket(NetworkChannel.OPERATION, new GameRuleUpdatePacket(worldName, ruleName, value));
                log.debug("Saved game rule {} = {} for scope {} and published to network", ruleName, value, scope);
            } catch (Exception e) {
                log.error("Failed to save game rule {} for scope {}: {}", ruleName, scope, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Removes a game rule from storage.
     * OPTIMIZED: Uses Lua script for atomic Redis update (single round-trip).
     *
     * @param worldName the world name (or null for global rules)
     * @param ruleName  the rule name
     * @return CompletableFuture that completes when removed
     */
    public CompletableFuture<Void> removeGameRule(@Nullable String worldName, @NotNull String ruleName) {
        final String scope = worldName != null ? worldName : GLOBAL_SCOPE;
        return CompletableFuture.runAsync(() -> {
            try {
                final Document filter = new Document("_id", scope);
                final Document update = new Document("$unset", new Document("rules." + ruleName, ""));
                collection.updateOne(filter, update);
                
                final String redisKey = buildRedisKey(scope);
                redisConnector.useJedis(jedis -> {
                    Object result = jedis.eval(LUA_REMOVE_RULE, 1, redisKey, ruleName, "3600");
                    if (result != null && ((Long) result) == 0L) {
                        log.debug("Cache for scope {} doesn't exist, will be loaded on next read", scope);
                    }
                });
                
                networkClient.sendNetworkPacket(NetworkChannel.OPERATION, new GameRuleUpdatePacket(worldName, ruleName, null));
                log.debug("Removed game rule {} for scope {}", ruleName, scope);
            } catch (Exception e) {
                log.error("Failed to remove game rule {} for scope {}: {}", ruleName, scope, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Builds a Redis key with proper prefix based on scope.
     * - Global rules: gamerule:shared:_global_ (shared across all servers)
     * - World rules: gamerule:{serverId}:{world} (isolated per server)
     */
    private String buildRedisKey(@NotNull String scope) {
        if (GLOBAL_SCOPE.equals(scope)) {
            // Global rules are shared across all servers
            return REDIS_KEY_PREFIX + SHARED_PREFIX + ":" + scope;
        } else {
            // World-specific rules are isolated per server
            return REDIS_KEY_PREFIX + serverId + ":" + scope;
        }
    }
    
    /**
     * Parses a value from storage to its correct type based on the game rule definition.
     */
    @Nullable
    private Object parseValue(@NotNull CustomGameRule<?> gameRule, @Nullable Object value) {
        if (value == null) {
            return null;
        }
        final Class<?> type = gameRule.type();
        if (type.isInstance(value)) {
            return value;
        }
        // Handle type conversions from MongoDB (which stores as Double/Integer/String)
        try {
            if (type == Boolean.class) {
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                }
            } else if (type == Integer.class && value instanceof Number) {
                return ((Number) value).intValue();
            } else if (type == Double.class && value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (type == Long.class && value instanceof Number) {
                return ((Number) value).longValue();
            } else if (type == Float.class && value instanceof Number) {
                return ((Number) value).floatValue();
            } else if (type == String.class) {
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("Failed to parse value {} for game rule {}, using raw value", value, gameRule.name(), e);
        }
        return value;
    }
    
    private String serializeGameRules(@NotNull Map<String, Object> rules) {
        return GSON.toJson(rules);
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeGameRules(@NotNull String json) {
        return GSON.fromJson(json, Map.class);
    }
}
