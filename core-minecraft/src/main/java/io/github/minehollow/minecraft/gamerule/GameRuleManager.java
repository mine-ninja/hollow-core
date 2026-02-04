package io.github.minehollow.minecraft.gamerule;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.gamerule.storage.GameRuleStorage;
import org.bukkit.Bukkit;
import org.bukkit.World;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages custom game rules across all worlds with multiserver support and persistence.
 * <p>
 * This manager handles only custom game rules, not vanilla Minecraft game rules.
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
        this.storage = new GameRuleStorage(platform.getNetworkClient(), platform.getExecutorService());
    }
    
    /**
     * Initializes the game rule manager with async loading and 10-second timeout.
     * <p>
     * Loads global rules and all world-specific rules in parallel.
     * Marks worlds as initializing to prevent duplicate loading from WorldLoadListener.
     * <p>
     * This method does not block the main thread and will continue even on timeout.
     */
    public void initialize() {
        log.debug("Initializing GameRuleManager with multi-server support...");
        try {
            List<CompletableFuture<Void>> loadingTasks = new ArrayList<>();
            CompletableFuture<Void> globalTask = storage.loadGameRules(null).thenAccept(globalRules -> {
                if (!globalRules.isEmpty()) {
                    globalGameRules.putAll(globalRules);
                    log.debug("Loaded {} global game rules", globalRules.size());
                } else {
                    log.debug("No global game rules found, using defaults");
                }
            }).exceptionally(ex -> {
                log.error("Failed to load global game rules: {}", ex.getMessage(), ex);
                return null;
            });
            loadingTasks.add(globalTask);
            for (World world : Bukkit.getWorlds()) {
                final String worldName = world.getName();
                if (initializingWorlds.add(worldName)) {
                    CompletableFuture<Void> worldTask = initializeWorldAsync(world);
                    loadingTasks.add(worldTask);
                } else {
                    log.debug("World {} already initializing, skipping", worldName);
                }
            }
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(loadingTasks.toArray(new CompletableFuture[0]));
            try {
                allTasks.get(10, TimeUnit.SECONDS);
                log.debug("GameRuleManager initialized successfully with {} registered game rules", GameRuleRegistry.getAllGameRules().size());
                initialized = true;
            } catch (Exception timeout) {
                log.warn("GameRuleManager initialization timed out after 10s, continuing with partial data");
                log.warn("Some game rules may not be loaded yet, will use defaults until loaded");
                initialized = true;
            }
        } catch (Exception e) {
            log.error("Failed to initialize GameRuleManager: {}", e.getMessage(), e);
            initialized = true;
        }
    }
    
    /**
     * Initializes a specific world's game rules asynchronously.
     * <p>
     * Thread-safe: Prevents duplicate initialization using concurrent sets.
     * Automatically moves world from "initializing" to "initialized" state.
     *
     * @param world the world to initialize
     *
     * @return CompletableFuture that completes when loading is done
     */
    private CompletableFuture<Void> initializeWorldAsync(@NotNull World world) {
        final String worldName = world.getName();
        if (initializedWorlds.contains(worldName)) {
            log.debug("World {} already initialized, skipping", worldName);
            return CompletableFuture.completedFuture(null);
        }
        if (!initializingWorlds.add(worldName)) {
            log.debug("World {} is already being initialized, skipping duplicate", worldName);
            return CompletableFuture.completedFuture(null);
        }
        log.debug("Loading custom game rules for world: {}", worldName);
        worldGameRules.putIfAbsent(worldName, new ConcurrentHashMap<>());
        return storage.loadGameRules(worldName).thenAccept(rules -> {
            if (!rules.isEmpty()) {
                worldGameRules.get(worldName).putAll(rules);
                log.debug("Loaded {} persisted game rules for world {}", rules.size(), worldName);
            } else {
                log.debug("No persisted game rules found for world {}, using defaults", worldName);
            }
            initializingWorlds.remove(worldName);
            initializedWorlds.add(worldName);
        }).exceptionally(ex -> {
            log.error("Failed to load game rules for world {}: {}", worldName, ex.getMessage(), ex);
            initializingWorlds.remove(worldName);
            return null;
        });
    }
    
    /**
     * Initializes a world's game rules. Safe to call multiple times.
     * <p>
     * Use this for lazy-loading worlds loaded after server startup.
     *
     * @param world the world to initialize
     */
    public void initializeWorld(@NotNull World world) {
        initializeWorldAsync(world);
    }
    
    /**
     * Checks if the manager has finished initializing.
     *
     * @return true if initialized (some data may still be loading in background)
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Handles game rule updates from other servers via network packets.
     * <p>
     * Updates the local cache without triggering database writes or network broadcasts.
     *
     * @param worldName the world name, or null for global rules
     * @param ruleName  the rule name
     * @param value     the new value, or null for removal
     */
    public void handleRemoteUpdate(@Nullable String worldName, @NotNull String ruleName, @Nullable Object value) {
        if (worldName == null) {
            log.debug("Received remote GLOBAL game rule update: {} = {}", ruleName, value);
            if (value == null) {
                globalGameRules.remove(ruleName.toLowerCase());
            } else {
                globalGameRules.put(ruleName.toLowerCase(), value);
            }
        } else {
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
     * Gets a game rule value, automatically detecting global vs world-specific.
     * <p>
     * If the manager is not yet initialized, returns the default value.
     *
     * @param world    the world (ignored if gameRule is global)
     * @param gameRule the game rule definition
     * @param <T>      the value type
     *
     * @return the current value, or default if not set or not initialized
     */
    @NotNull
    public <T> T getGameRule(@NotNull World world, @NotNull CustomGameRule<T> gameRule) {
        if (!isInitialized()) {
            log.warn("GameRuleManager not initialized yet, returning default value for {}", gameRule.name());
            return gameRule.defaultValue();
        }
        
        if (gameRule.global()) {
            return getGlobalGameRule(gameRule);
        } else {
            return getWorldGameRule(world, gameRule);
        }
    }
    
    /**
     * Gets a global game rule value.
     * <p>
     * If the manager is not yet initialized, returns the default value.
     *
     * @param gameRule the game rule definition
     * @param <T>      the value type
     *
     * @return the current value, or default if not set or not initialized
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getGlobalGameRule(@NotNull CustomGameRule<T> gameRule) {
        if (!isInitialized()) {
            log.warn("GameRuleManager not initialized yet, returning default value for {}", gameRule.name());
            return gameRule.defaultValue();
        }
        
        final T value = (T) globalGameRules.get(gameRule.name().toLowerCase());
        return value != null ? value : gameRule.defaultValue();
    }
    
    /**
     * Gets a world-specific game rule value.
     * <p>
     * If the manager is not yet initialized, returns the default value.
     *
     * @param world    the world
     * @param gameRule the game rule definition
     * @param <T>      the value type
     *
     * @return the current value, or default if not set or not initialized
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getWorldGameRule(@NotNull World world, @NotNull CustomGameRule<T> gameRule) {
        if (!isInitialized()) {
            log.warn("GameRuleManager not initialized yet, returning default value for {}", gameRule.name());
            return gameRule.defaultValue();
        }
        
        final Map<String, Object> worldRules = worldGameRules.get(world.getName());
        if (worldRules == null) {
            return gameRule.defaultValue();
        }
        final T value = (T) worldRules.get(gameRule.name().toLowerCase());
        return value != null ? value : gameRule.defaultValue();
    }
    
    /**
     * Sets a game rule value, automatically detecting global vs world-specific.
     * <p>
     * Persists to MongoDB, updates Redis cache, and broadcasts to other servers.
     * <p>
     * If the manager is not yet initialized, the operation is queued and will execute after initialization.
     *
     * @param world    the world (ignored if gameRule is global)
     * @param gameRule the game rule definition
     * @param value    the new value
     * @param <T>      the value type
     */
    public <T> void setGameRule(@NotNull World world, @NotNull CustomGameRule<T> gameRule, @NotNull T value) {
        if (!isInitialized()) {
            log.warn("GameRuleManager not initialized yet, but allowing set operation for {}", gameRule.name());
        }
        
        if (gameRule.global()) {
            setGlobalGameRule(gameRule, value);
        } else {
            setWorldGameRule(world, gameRule, value);
        }
    }
    
    /**
     * Sets a global game rule value.
     * <p>
     * Persists to MongoDB, updates Redis cache, and broadcasts to all servers.
     *
     * @param gameRule the game rule definition
     * @param value    the new value
     * @param <T>      the value type
     */
    public <T> void setGlobalGameRule(@NotNull CustomGameRule<T> gameRule, @NotNull T value) {
        final String normalizedName = gameRule.name().toLowerCase();
        globalGameRules.put(normalizedName, value);
        storage.saveGameRule(null, normalizedName, value).exceptionally(ex -> {
            log.error("Failed to persist global game rule {}: {}", gameRule.name(), ex.getMessage());
            return null;
        });
        log.debug("Set GLOBAL game rule {} = {}", gameRule.name(), value);
    }
    
    /**
     * Sets a world-specific game rule value.
     * <p>
     * Persists to MongoDB, updates Redis cache, and broadcasts to all servers.
     *
     * @param world    the world
     * @param gameRule the game rule definition
     * @param value    the new value
     * @param <T>      the value type
     */
    public <T> void setWorldGameRule(@NotNull World world, @NotNull CustomGameRule<T> gameRule, @NotNull T value) {
        final String worldName = world.getName();
        final String normalizedName = gameRule.name().toLowerCase();
        worldGameRules.putIfAbsent(worldName, new ConcurrentHashMap<>());
        worldGameRules.get(worldName).put(normalizedName, value);
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
        return getCustomGameRule(Bukkit.getWorlds().getFirst(), ruleName);
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
        worldGameRules.putIfAbsent(worldName, new ConcurrentHashMap<>());
        worldGameRules.get(worldName).put(normalizedName, value);
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
            storage.removeGameRule(world.getName(), ruleName.toLowerCase()).exceptionally(ex -> {
                log.error("Failed to persist game rule removal for {}: {}", ruleName, ex.getMessage());
                return null;
            });
            return true;
        }
        return false;
    }
}
