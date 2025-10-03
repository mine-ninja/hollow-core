package net.warcane.lugin.core.minecraft.gamerule;

import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.gamerule.storage.GameRuleStorage;
import org.bukkit.Bukkit;
import org.bukkit.World;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages custom game rules across all worlds with multi-server support and persistence.
 * Provides a unified interface for getting and setting custom game rules.
 * <p>
 * This manager does NOT handle vanilla Minecraft game rules - it only manages custom ones.
 * <p>
 * Features:
 * - Multi-server synchronization via Redis pub/sub
 * - Persistent storage in MongoDB
 * - Redis caching for fast access
 * - Automatic cross-server updates
 * - Non-blocking async initialization with timeout
 * - Protection against duplicate world initialization
 */
@Slf4j
public class GameRuleManager {
    private final Map<String, Map<String, Object>> worldGameRules = new ConcurrentHashMap<>();
    private final Map<String, Object> globalGameRules = new ConcurrentHashMap<>();
    private final Set<String> initializingWorlds = ConcurrentHashMap.newKeySet();
    private final Set<String> initializedWorlds = ConcurrentHashMap.newKeySet();
    private final GameRuleStorage storage;
    private volatile boolean initialized = false;
    
    public GameRuleManager(@NotNull BukkitPlatform platform) {
        this.storage = new GameRuleStorage(
            platform.getNetworkClient(),
            platform.getExecutorService()
        );
    }
    
    /**
     * Initializes the game rule manager.
     * Should be called during platform initialization.
     * Uses async loading with timeout to avoid blocking the main thread.
     */
    public void initialize() {
        log.info("Initializing GameRuleManager with multi-server support...");
        
        try {
            // Collect all async loading tasks
            List<CompletableFuture<Void>> loadingTasks = new ArrayList<>();
            
            // Load global game rules (ASYNC)
            CompletableFuture<Void> globalTask = storage.loadGameRules(null)
                .thenAccept(globalRules -> {
                    if (!globalRules.isEmpty()) {
                        globalGameRules.putAll(globalRules);
                        log.info("Loaded {} global game rules", globalRules.size());
                    } else {
                        log.info("No global game rules found, using defaults");
                    }
                })
                .exceptionally(ex -> {
                    log.error("Failed to load global game rules: {}", ex.getMessage(), ex);
                    return null;
                });
            loadingTasks.add(globalTask);
            
            // Load world-specific rules for all loaded worlds (ASYNC)
            // Mark worlds as "initializing" to prevent duplicate loads from WorldLoadListener
            for (World world : Bukkit.getWorlds()) {
                final String worldName = world.getName();
                if (initializingWorlds.add(worldName)) {
                    CompletableFuture<Void> worldTask = initializeWorldAsync(world);
                    loadingTasks.add(worldTask);
                } else {
                    log.debug("World {} already initializing, skipping", worldName);
                }
            }
            
            // Wait for all loading tasks to complete with timeout (non-blocking for individual tasks)
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                loadingTasks.toArray(new CompletableFuture[0])
            );
            
            // Wait with timeout to avoid indefinite blocking
            try {
                allTasks.get(10, TimeUnit.SECONDS);
                log.info("GameRuleManager initialized successfully with {} registered game rules",
                         GameRuleRegistry.getAllGameRules().size());
                initialized = true;
            } catch (Exception timeout) {
                log.warn("GameRuleManager initialization timed out after 10s, continuing with partial data");
                log.warn("Some game rules may not be loaded yet, will use defaults until loaded");
                initialized = true; // Mark as initialized anyway to allow server to start
            }
            
        } catch (Exception e) {
            log.error("Failed to initialize GameRuleManager: {}", e.getMessage(), e);
            initialized = true; // Allow server to start with defaults
        }
    }
    
    /**
     * Initializes custom game rules storage for a specific world (ASYNC).
     * Returns a CompletableFuture that completes when loading is done.
     *
     * THREAD-SAFE: Prevents duplicate initialization of the same world.
     *
     * @param world the world to initialize
     * @return CompletableFuture that completes when world rules are loaded
     */
    private CompletableFuture<Void> initializeWorldAsync(@NotNull World world) {
        final String worldName = world.getName();
        
        // Check if already initialized
        if (initializedWorlds.contains(worldName)) {
            log.debug("World {} already initialized, skipping", worldName);
            return CompletableFuture.completedFuture(null);
        }
        
        // Check if currently initializing
        if (!initializingWorlds.add(worldName)) {
            log.debug("World {} is already being initialized, skipping duplicate", worldName);
            return CompletableFuture.completedFuture(null);
        }
        
        log.debug("Loading custom game rules for world: {}", worldName);
        
        // Initialize in-memory storage
        worldGameRules.putIfAbsent(worldName, new ConcurrentHashMap<>());
        
        // Load persisted values from database (ASYNC)
        return storage.loadGameRules(worldName)
            .thenAccept(rules -> {
                if (!rules.isEmpty()) {
                    worldGameRules.get(worldName).putAll(rules);
                    log.info("Loaded {} persisted game rules for world {}", rules.size(), worldName);
                } else {
                    log.debug("No persisted game rules found for world {}, using defaults", worldName);
                }
                // Mark as initialized
                initializingWorlds.remove(worldName);
                initializedWorlds.add(worldName);
            })
            .exceptionally(ex -> {
                log.error("Failed to load game rules for world {}: {}", worldName, ex.getMessage(), ex);
                // Remove from initializing even on error to allow retry
                initializingWorlds.remove(worldName);
                return null;
            });
    }
    
    /**
     * Initializes custom game rules storage for a specific world (ASYNC).
     * Loads persisted values from database asynchronously.
     * Use this for lazy-loading worlds that are loaded after server startup.
     *
     * THREAD-SAFE: Can be called multiple times safely, will only initialize once.
     *
     * @param world the world to initialize
     */
    public void initializeWorld(@NotNull World world) {
        initializeWorldAsync(world); // Delegate to async method with duplicate protection
    }
    
    /**
     * Checks if the GameRuleManager has finished initializing.
     *
     * @return true if initialized (may still be loading some data in background)
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Handles game rule updates from other servers.
     * This is called by the GameRuleUpdateListener when updates arrive via the network.
     *
     * @param worldName the world name (null for global rules)
     * @param ruleName the rule name
     * @param value the new value (null means removal)
     */
    public void handleRemoteUpdate(@Nullable String worldName, @NotNull String ruleName, @Nullable Object value) {
        if (worldName == null) {
            // Global rule update
            log.debug("Received remote GLOBAL game rule update: {} = {}", ruleName, value);
            if (value == null) {
                globalGameRules.remove(ruleName.toLowerCase());
            } else {
                globalGameRules.put(ruleName.toLowerCase(), value);
            }
        } else {
            // World-specific rule update
            log.debug("Received remote game rule update: {} = {} for world {}", ruleName, value, worldName);
            final Map<String, Object> worldRules = worldGameRules.get(worldName);
            if (worldRules != null) {
                if (value == null) {
                    worldRules.remove(ruleName.toLowerCase());
                } else {
                    worldRules.put(ruleName.toLowerCase(), value);
                }
            }
        }
    }
    
    /**
     * Gets a game rule value using the GameRule object.
     * Automatically determines if it's global or world-specific.
     *
     * @param world the world (ignored if gameRule is global)
     * @param gameRule the game rule
     * @param <T> the type of the value
     * @return the value, or default value if not set
     */
    @NotNull
    public <T> T getGameRule(@NotNull World world, @NotNull CustomGameRule<T> gameRule) {
        if (gameRule.global()) {
            return getGlobalGameRule(gameRule);
        } else {
            return getWorldGameRule(world, gameRule);
        }
    }
    
    /**
     * Gets a global game rule value.
     *
     * @param gameRule the game rule
     * @param <T> the type of the value
     * @return the value, or default value if not set
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getGlobalGameRule(@NotNull CustomGameRule<T> gameRule) {
        final T value = (T) globalGameRules.get(gameRule.name().toLowerCase());
        return value != null ? value : gameRule.defaultValue();
    }
    
    /**
     * Gets a world-specific game rule value.
     *
     * @param world the world
     * @param gameRule the game rule
     * @param <T> the type of the value
     * @return the value, or default value if not set
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getWorldGameRule(@NotNull World world, @NotNull CustomGameRule<T> gameRule) {
        final Map<String, Object> worldRules = worldGameRules.get(world.getName());
        if (worldRules == null) {
            return gameRule.defaultValue();
        }
        final T value = (T) worldRules.get(gameRule.name().toLowerCase());
        return value != null ? value : gameRule.defaultValue();
    }
    
    /**
     * Sets a game rule value using the GameRule object.
     * Automatically determines if it's global or world-specific.
     *
     * @param world the world (ignored if gameRule is global)
     * @param gameRule the game rule
     * @param value the value to set
     * @param <T> the type of the value
     */
    public <T> void setGameRule(@NotNull World world, @NotNull CustomGameRule<T> gameRule, @NotNull T value) {
        if (gameRule.global()) {
            setGlobalGameRule(gameRule, value);
        } else {
            setWorldGameRule(world, gameRule, value);
        }
    }
    
    /**
     * Sets a global game rule value.
     * This will persist to database and sync across all servers.
     *
     * @param gameRule the game rule
     * @param value the value to set
     * @param <T> the type of the value
     */
    public <T> void setGlobalGameRule(@NotNull CustomGameRule<T> gameRule, @NotNull T value) {
        final String normalizedName = gameRule.name().toLowerCase();
        // Update local cache
        globalGameRules.put(normalizedName, value);
        // Persist to database and sync to other servers (worldName = null for global)
        storage.saveGameRule(null, normalizedName, value).exceptionally(ex -> {
            log.error("Failed to persist global game rule {}: {}", gameRule.name(), ex.getMessage());
            return null;
        });
        log.debug("Set GLOBAL game rule {} = {}", gameRule.name(), value);
    }
    
    /**
     * Sets a world-specific game rule value.
     * This will persist to database and sync across all servers.
     *
     * @param world the world
     * @param gameRule the game rule
     * @param value the value to set
     * @param <T> the type of the value
     */
    public <T> void setWorldGameRule(@NotNull World world, @NotNull CustomGameRule<T> gameRule, @NotNull T value) {
        final String worldName = world.getName();
        final String normalizedName = gameRule.name().toLowerCase();
        // Update local cache
        worldGameRules.putIfAbsent(worldName, new ConcurrentHashMap<>());
        worldGameRules.get(worldName).put(normalizedName, value);
        // Persist to database and sync to other servers
        storage.saveGameRule(worldName, normalizedName, value).exceptionally(ex -> {
            log.error("Failed to persist game rule {} for world {}: {}", gameRule.name(), worldName, ex.getMessage());
            return null;
        });
        log.debug("Set game rule {} = {} for world {}", gameRule.name(), value, worldName);
    }
    
    /**
     * Gets a custom game rule value for the default world.
     *
     * @param ruleName the name of the custom game rule
     * @param <T>      the type of the value
     *
     * @return the value, or null if not set
     */
    @Nullable
    public <T> T getCustomGameRule(@NotNull String ruleName) {
        return getCustomGameRule(Bukkit.getWorlds().get(0), ruleName);
    }
    
    /**
     * Gets a custom game rule value for a specific world.
     *
     * @param world    the world
     * @param ruleName the name of the custom game rule
     * @param <T>      the type of the value
     *
     * @return the value, or null if not set
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getCustomGameRule(@NotNull World world, @NotNull String ruleName) {
        final Map<String, Object> worldRules = worldGameRules.get(world.getName());
        if (worldRules == null) {
            return null;
        }
        return (T) worldRules.get(ruleName.toLowerCase());
    }
    
    /**
     * Gets a custom game rule value with a default fallback.
     *
     * @param ruleName     the name of the custom game rule
     * @param defaultValue the default value if not set
     * @param <T>          the type of the value
     *
     * @return the value, or default if not set
     */
    @NotNull
    public <T> T getCustomGameRuleOrDefault(@NotNull String ruleName, @NotNull T defaultValue) {
        final T value = getCustomGameRule(ruleName);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets a custom game rule value with a default fallback for a specific world.
     *
     * @param world        the world
     * @param ruleName     the name of the custom game rule
     * @param defaultValue the default value if not set
     * @param <T>          the type of the value
     *
     * @return the value, or default if not set
     */
    @NotNull
    public <T> T getCustomGameRuleOrDefault(@NotNull World world, @NotNull String ruleName, @NotNull T defaultValue) {
        final T value = getCustomGameRule(world, ruleName);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Sets a custom game rule value for all worlds.
     * This will persist to database and sync across all servers.
     *
     * @param ruleName the name of the custom game rule
     * @param type     the type class
     * @param value    the value to set
     * @param <T>      the type of the value
     */
    public <T> void setCustomGameRule(@NotNull String ruleName, @NotNull Class<T> type, @NotNull T value) {
        for (World world : Bukkit.getWorlds()) {
            setCustomGameRule(world, ruleName, type, value);
        }
    }
    
    /**
     * Sets a custom game rule value for a specific world.
     * This will persist to database and sync across all servers.
     *
     * @param world    the world
     * @param ruleName the name of the custom game rule
     * @param type     the type class
     * @param value    the value to set
     * @param <T>      the type of the value
     */
    public <T> void setCustomGameRule(@NotNull World world, @NotNull String ruleName, @NotNull Class<T> type, @NotNull T value) {
        final String worldName = world.getName();
        final String normalizedName = ruleName.toLowerCase();
        // Update local cache
        worldGameRules.putIfAbsent(worldName, new ConcurrentHashMap<>());
        worldGameRules.get(worldName).put(normalizedName, value);
        // Persist to database and sync to other servers
        storage.saveGameRule(worldName, normalizedName, value).exceptionally(ex -> {
            log.error("Failed to persist game rule {} for world {}: {}", ruleName, worldName, ex.getMessage());
            return null;
        });
        log.debug("Set custom game rule {} = {} for world {}", ruleName, value, worldName);
    }
    
    /**
     * Gets all custom game rules for a world.
     *
     * @param world the world
     *
     * @return map of rule names to values
     */
    @NotNull
    public Map<String, Object> getAllCustomGameRules(@NotNull World world) {
        return new HashMap<>(worldGameRules.getOrDefault(world.getName(), new HashMap<>()));
    }
    
    /**
     * Checks if a custom game rule is set for a world.
     *
     * @param world    the world
     * @param ruleName the name of the custom game rule
     *
     * @return true if set, false otherwise
     */
    public boolean hasCustomGameRule(@NotNull World world, @NotNull String ruleName) {
        final Map<String, Object> worldRules = worldGameRules.get(world.getName());
        return worldRules != null && worldRules.containsKey(ruleName.toLowerCase());
    }
    
    /**
     * Removes a custom game rule from a world.
     * This will persist to database and sync across all servers.
     *
     * @param world    the world
     * @param ruleName the name of the custom game rule
     *
     * @return true if removed, false if not found
     */
    public boolean removeCustomGameRule(@NotNull World world, @NotNull String ruleName) {
        final Map<String, Object> worldRules = worldGameRules.get(world.getName());
        if (worldRules == null) {
            return false;
        }
        final Object removed = worldRules.remove(ruleName.toLowerCase());
        if (removed != null) {
            // Persist removal to database and sync to other servers
            storage.removeGameRule(world.getName(), ruleName.toLowerCase()).exceptionally(ex -> {
                log.error("Failed to persist game rule removal for {}: {}", ruleName, ex.getMessage());
                return null;
            });
            return true;
        }
        return false;
    }
}
