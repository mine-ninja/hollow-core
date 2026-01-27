package io.github.minehollow.minecraft.gamerule;

import io.github.minehollow.minecraft.BukkitPlatform;
import org.bukkit.World;

/**
 * Example usage of the custom game rule system.
 *
 * This class demonstrates how to:
 * 1. Register custom game rules
 * 2. Get and set custom game rule values
 * 3. Use custom game rules in your plugin logic
 *
 * NOTE: This system does NOT manage vanilla Minecraft game rules.
 * Use Bukkit's World.setGameRule() for vanilla rules like keepInventory, doDaylightCycle, etc.
 *
 * NEW FEATURES:
 * - Multi-server synchronization: Changes on one server automatically sync to all others via Redis
 * - Persistence: All game rules are saved to MongoDB and survive server restarts
 * - Caching: Redis caching for fast access across your network
 */
public class CustomGameRuleExample {
    
    /**
     * Example of how to register custom game rules during plugin initialization.
     * Call this method early in your plugin's onEnable().
     */
    public static void registerExampleGameRules() {
        // Register a boolean game rule
        GameRuleRegistry.register(new CustomGameRule<>(
            "disableHunger",
            Boolean.class,
            false,
            "Disables hunger depletion for all players"
        ));
        
        // Register an integer game rule
        GameRuleRegistry.register(new CustomGameRule<>(
            "maxPlayersPerFaction",
            Integer.class,
            10,
            "Maximum number of players allowed in a faction"
        ));
        
        // Register a string game rule
        GameRuleRegistry.register(new CustomGameRule<>(
            "factionPrefix",
            String.class,
            "&7[&6Faction&7]",
            "Prefix displayed before faction names in chat"
        ));
        
        // Register a double game rule
        GameRuleRegistry.register(new CustomGameRule<>(
            "experienceMultiplier",
            Double.class,
            1.0,
            "Multiplier for experience gained"
        ));
    }
    
    /**
     * Example of how to use custom game rules in your plugin logic.
     *
     * When you set a value, it will:
     * 1. Update the local cache immediately
     * 2. Save to MongoDB for persistence
     * 3. Publish to Redis so other servers receive the update
     * 4. Cache in Redis for 1 hour for fast cross-server access
     */
    public static void exampleUsage(BukkitPlatform platform, World world) {
        final GameRuleManager manager = platform.getGameRuleManager();
        
        // Get a custom game rule value (checks local cache, then loads from DB if needed)
        Boolean disableHunger = manager.getCustomGameRule(world, "disableHunger");
        if (disableHunger != null && disableHunger) {
            // Apply hunger disabling logic
            System.out.println("Hunger is disabled in world: " + world.getName());
        }
        
        // Get with default fallback
        Integer maxPlayers = manager.getCustomGameRuleOrDefault(world, "maxPlayersPerFaction", 10);
        System.out.println("Max players per faction: " + maxPlayers);
        
        // Set a custom game rule for a specific world
        // This will save to MongoDB AND sync to all other servers automatically
        manager.setCustomGameRule(world, "disableHunger", Boolean.class, true);
        
        // Set a custom game rule for all worlds
        // This will sync across ALL servers in your network
        manager.setCustomGameRule("maxPlayersPerFaction", Integer.class, 15);
        
        // Check if a custom game rule is set
        if (manager.hasCustomGameRule(world, "experienceMultiplier")) {
            Double multiplier = manager.getCustomGameRule(world, "experienceMultiplier");
            System.out.println("Experience multiplier: " + multiplier);
        }
    }
    
    /**
     * How multi-server sync works:
     *
     * Server A sets a game rule:
     *   1. Updates local cache
     *   2. Saves to MongoDB
     *   3. Publishes update to Redis channel "gamerule-updates"
     *
     * Server B, C, D automatically:
     *   1. Receive the update via Redis subscription
     *   2. Update their local caches
     *   3. Can immediately access the new value
     *
     * On server restart:
     *   1. Loads all game rules from MongoDB
     *   2. Populates local cache
     *   3. Subscribes to Redis for future updates
     */
    
    /**
     * For vanilla Minecraft game rules, use Bukkit's built-in API:
     *
     * world.setGameRule(GameRule.KEEP_INVENTORY, true);
     * world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
     * world.setGameRule(GameRule.DO_MOB_SPAWNING, true);
     *
     * This custom system is ONLY for your own custom game rules that don't exist in vanilla Minecraft.
     */
}
