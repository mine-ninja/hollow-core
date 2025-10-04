package net.warcane.lugin.core.minecraft.gamerule;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for custom game rules.
 * <p>
 * Allows plugins to register their own game rules that can be managed
 * alongside vanilla Minecraft game rules.
 */
public class GameRuleRegistry {
    private static final Map<String, CustomGameRule<?>> REGISTERED_RULES = new ConcurrentHashMap<>();
    
    /**
     * Registers a custom game rule.
     *
     * @param gameRule the game rule to register
     * @param <T>      the value type
     *
     * @throws IllegalStateException if a game rule with the same name is already registered
     */
    public static <T> void register(@NotNull CustomGameRule<T> gameRule) {
        final String name = gameRule.name().toLowerCase();
        if (REGISTERED_RULES.containsKey(name)) {
            throw new IllegalStateException("Game rule '" + name + "' is already registered");
        }
        REGISTERED_RULES.put(name, gameRule);
    }
    
    /**
     * Gets a registered custom game rule by name.
     *
     * @param name the name of the game rule
     *
     * @return the game rule, or null if not found
     */
    @Nullable
    public static CustomGameRule<?> getGameRule(@NotNull String name) {
        return REGISTERED_RULES.get(name.toLowerCase());
    }
    
    /**
     * Checks if a custom game rule is registered.
     *
     * @param name the name of the game rule
     *
     * @return true if registered, false otherwise
     */
    public static boolean isRegistered(@NotNull String name) {
        return REGISTERED_RULES.containsKey(name.toLowerCase());
    }
    
    /**
     * Gets all registered custom game rules.
     *
     * @return immutable collection of all registered game rules
     */
    @NotNull
    public static Collection<CustomGameRule<?>> getAllGameRules() {
        return REGISTERED_RULES.values();
    }
    
    /**
     * Unregisters a custom game rule.
     *
     * @param name the name of the game rule to unregister
     *
     * @return true if unregistered, false if not found
     */
    public static boolean unregister(@NotNull String name) {
        return REGISTERED_RULES.remove(name.toLowerCase()) != null;
    }
    
    /**
     * Clears all registered custom game rules.
     * <p>
     * Warning: This should only be used during shutdown or testing.
     */
    public static void clear() {
        REGISTERED_RULES.clear();
    }
}
