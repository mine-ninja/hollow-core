package net.warcane.lugin.core.minecraft.gamerule.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.gamerule.CustomGameRule;
import net.warcane.lugin.core.minecraft.gamerule.GameRuleRegistry;
import net.warcane.lugin.core.network.NetworkClient;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.gamerule.GameRuleUpdatePacket;
import net.warcane.lugin.core.util.property.Property;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
 * <p>
 * Uses MongoDB for persistent storage and Redis for caching.
 * Cross-server sync is handled via NetworkClient using the OPERATION channel.
 * <p>
 * Cache Strategy:
 * <ul>
 *   <li>World-specific rules: Isolated cache per server (gamerule:{serverId}:{world})</li>
 *   <li>Global rules: Shared cache across all servers (gamerule:shared:_global_)</li>
 * </ul>
 */
@Slf4j
public class GameRuleStorage {
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, false)
        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, false);
    
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};
    
    private static final String REDIS_KEY_PREFIX = "gamerule:";
    private static final String SHARED_PREFIX = "shared";
    private static final String GLOBAL_SCOPE = "_global_";
    private static final String MONGO_COLLECTION = "game_rules";
    
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
    
    public GameRuleStorage(@NotNull NetworkClient networkClient, @NotNull ExecutorService executorService) {
        this.redisConnector = RedisConnector.getInstance();
        this.collection = MongoDbConnector.getInstance().getCollection(MONGO_COLLECTION, Document.class);
        this.networkClient = networkClient;
        this.executorService = executorService;
    }
    
    /**
     * Loads all game rules for a specific scope from storage.
     * <p>
     * Attempts to load from Redis cache first, falls back to MongoDB if cache miss.
     * Automatically caches MongoDB results in Redis with 1-hour TTL.
     *
     * @param worldName the world name, or null for global rules
     * @return CompletableFuture with map of rule names to values
     */
    public CompletableFuture<Map<String, Object>> loadGameRules(@Nullable String worldName) {
        final String scope = worldName != null ? worldName : GLOBAL_SCOPE;
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String redisKey = buildScopeKey(scope);
                final String cached = redisConnector.supplyFromJedis(jedis -> jedis.get(redisKey));
                if (cached != null && !cached.isEmpty()) {
                    log.debug("Loaded game rules for scope {} from Redis cache", scope);
                    return deserializeGameRules(cached);
                }
                
                final Document doc = collection.find(Filters.eq("_id", scope)).first();
                if (doc == null) {
                    log.debug("No game rules found for scope {} in database", scope);
                    return new HashMap<>();
                }
                
                @SuppressWarnings("unchecked")
                final Map<String, Object> rules = (Map<String, Object>) doc.get("rules");
                final Map<String, Object> parsedRules = new HashMap<>();
                
                if (rules != null) {
                    for (Map.Entry<String, Object> entry : rules.entrySet()) {
                        final CustomGameRule<?> gameRule = GameRuleRegistry.getGameRule(entry.getKey());
                        if (gameRule != null) {
                            parsedRules.put(entry.getKey(), parseValue(gameRule, entry.getValue()));
                        }
                    }
                    
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
     * Saves a game rule value to MongoDB, updates Redis cache, and broadcasts to other servers.
     * <p>
     * Uses Lua script for atomic Redis update (single round-trip).
     *
     * @param worldName the world name, or null for global rules
     * @param ruleName the rule name
     * @param value the value to save
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Void> saveGameRule(@Nullable String worldName, @NotNull String ruleName, @NotNull Object value) {
        final String scope = worldName != null ? worldName : GLOBAL_SCOPE;
        if (value instanceof Location location) {
            value = location.getWorld().getName() + ";" + location.getX() + ";" + location.getY() + ";" + location.getZ();
        }
        
        final Object finalValue = value;
        return CompletableFuture.runAsync(() -> {
            try {
                final String scopeKey = buildScopeKey(scope);
                
                final Document filter = new Document("_id", scopeKey);
                final Document update = new Document("$set",
                    new Document("rules." + ruleName, finalValue)
                        .append("updatedAt", System.currentTimeMillis())
                );
                collection.updateOne(filter, update, new UpdateOptions().upsert(true));
                
                final String valueJson = MAPPER.writeValueAsString(finalValue);
                
                redisConnector.useJedis(jedis -> {
                    Object result = jedis.eval(LUA_UPDATE_RULE, 1, scopeKey, ruleName, valueJson, "3600");
                    if (result != null && ((Long) result) == 0L) {
                        try {
                            log.debug("Cache for scope {} doesn't exist, creating with rule {}", scopeKey, ruleName);
                            
                            Map<String, Object> newCache = new HashMap<>();
                            newCache.put(ruleName, finalValue);
                            
                            String cacheJson = MAPPER.writeValueAsString(newCache);
                            jedis.set(scopeKey, cacheJson, SetParams.setParams().ex(3600));
                        } catch (JsonProcessingException e) {
                            log.error("Failed to serialize game rule {} for Redis cache: {}", ruleName, e.getMessage(), e);
                            throw new RuntimeException(e);
                        }
                    }
                });
                
                networkClient.sendNetworkPacket(NetworkChannel.OPERATION, new GameRuleUpdatePacket(worldName, ruleName, finalValue));
                log.debug("Saved game rule {} = {} for scope {} and published to network", ruleName, finalValue, scopeKey);
            } catch (Exception e) {
                log.error("Failed to save game rule {} for scope {}: {}", ruleName, scope, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Removes a game rule from MongoDB, updates Redis cache, and broadcasts to other servers.
     * <p>
     * Uses Lua script for atomic Redis update (single round-trip).
     *
     * @param worldName the world name, or null for global rules
     * @param ruleName the rule name
     * @return CompletableFuture that completes when removed
     */
    public CompletableFuture<Void> removeGameRule(@Nullable String worldName, @NotNull String ruleName) {
        final String scope = worldName != null ? worldName : GLOBAL_SCOPE;
        return CompletableFuture.runAsync(() -> {
            try {
                final String scopeKey = buildScopeKey(scope);
                
                final Document filter = new Document("_id", scopeKey);
                final Document update = new Document("$unset", new Document("rules." + ruleName, ""));
                collection.updateOne(filter, update);
                
                redisConnector.useJedis(jedis -> {
                    Object result = jedis.eval(LUA_REMOVE_RULE, 1, scopeKey, ruleName, "3600");
                    if (result != null && ((Long) result) == 0L) {
                        jedis.del(scopeKey);
                    }
                });
                
                networkClient.sendNetworkPacket(NetworkChannel.OPERATION, new GameRuleUpdatePacket(worldName, ruleName, null));
                log.debug("Removed game rule {} for scope {}", ruleName, scopeKey);
            } catch (Exception e) {
                log.error("Failed to remove game rule {} for scope {}: {}", ruleName, scope, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }
    
    /**
     * Builds the Redis key for a scope.
     * <p>
     * Global rules use shared key across servers, world rules are isolated per server.
     *
     * @param scope the scope (_global_ or world name)
     * @return the Redis key
     */
    private String buildScopeKey(@NotNull String scope) {
        if (GLOBAL_SCOPE.equals(scope)) {
            return REDIS_KEY_PREFIX + SHARED_PREFIX + ":" + scope;
        } else {
            return REDIS_KEY_PREFIX + BukkitPlatform.getInstance().getId() + ":" + scope;
        }
    }
    
    /**
     * Parses a value from storage to its correct type based on the game rule definition.
     *
     * @param gameRule the game rule definition
     * @param value the raw value from storage
     * @return the parsed value, or null if parsing fails
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
        
        try {
            if (type == Boolean.class) {
                if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                }
            }
            else if (type == Integer.class && value instanceof Number) {
                return ((Number) value).intValue();
            }
            else if (type == Double.class && value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            else if (type == Long.class && value instanceof Number) {
                return ((Number) value).longValue();
            }
            else if (type == Float.class && value instanceof Number) {
                return ((Number) value).floatValue();
            }
            else if (type == String.class) {
                return value.toString();
            }
            else if (type == Location.class) {
                String[] split = ((String) value).split(";");
                return new Location(
                    Bukkit.getWorld(split[0]),
                    Double.parseDouble(split[1]),
                    Double.parseDouble(split[2]),
                    Double.parseDouble(split[3])
                );
            }
        } catch (Exception e) {
            log.warn("Failed to parse value {} for game rule {}, using raw value", value, gameRule.name(), e);
        }
        
        return value;
    }
    
    private String serializeGameRules(@NotNull Map<String, Object> rules) {
        try {
            return MAPPER.writeValueAsString(rules);
        } catch (Exception e) {
            log.error("Failed to serialize game rules: {}", e.getMessage(), e);
            return "{}";
        }
    }
    
    private Map<String, Object> deserializeGameRules(@NotNull String json) {
        try {
            return MAPPER.readValue(json, MAP_TYPE_REF);
        } catch (Exception e) {
            log.error("Failed to deserialize game rules: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }
}
