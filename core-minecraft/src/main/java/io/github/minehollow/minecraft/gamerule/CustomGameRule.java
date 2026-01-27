package io.github.minehollow.minecraft.gamerule;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a custom game rule with a name, type, default value, and optional global scope.
 * <p>
 * Custom game rules can be either world-specific or global (server-wide).
 *
 * @param name         the unique name of the game rule
 * @param type         the value type (Boolean, Integer, String, etc.)
 * @param defaultValue the default value when not set
 * @param description  optional description of the rule
 * @param global       whether this rule applies globally across all worlds
 * @param <T>          the type of the game rule value
 */
public record CustomGameRule<T>(String name, Class<T> type, T defaultValue, String description, boolean global) {
    public CustomGameRule(@NotNull String name, @NotNull Class<T> type, @NotNull T defaultValue, @NotNull String description, boolean global) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
        this.global = global;
    }
    
    public CustomGameRule(@NotNull String name, @NotNull Class<T> type, @NotNull T defaultValue, @NotNull String description) {
        this(name, type, defaultValue, description, false);
    }
    
    public CustomGameRule(@NotNull String name, @NotNull Class<T> type, @NotNull T defaultValue) {
        this(name, type, defaultValue, "", false);
    }
    
    /**
     * Creates a global game rule that applies to all worlds across all servers.
     *
     * @param name         the unique name
     * @param type         the value type
     * @param defaultValue the default value
     * @param description  the description
     * @param <T>          the value type
     *
     * @return a global game rule
     */
    public static <T> CustomGameRule<T> global(@NotNull String name, @NotNull Class<T> type, @NotNull T defaultValue, @NotNull String description) {
        return new CustomGameRule<>(name, type, defaultValue, description, true);
    }
    
    /**
     * Creates a global game rule that applies to all worlds across all servers.
     *
     * @param name         the unique name
     * @param type         the value type
     * @param defaultValue the default value
     * @param <T>          the value type
     *
     * @return a global game rule
     */
    public static <T> CustomGameRule<T> global(@NotNull String name, @NotNull Class<T> type, @NotNull T defaultValue) {
        return new CustomGameRule<>(name, type, defaultValue, "", true);
    }
    
    /**
     * Validates if the provided value is compatible with this game rule's type.
     *
     * @param value the value to validate
     *
     * @return true if valid, false otherwise
     */
    public boolean isValidValue(@NotNull Object value) {
        return type.isInstance(value);
    }
    
    /**
     * Parses a string value to the appropriate type for this game rule.
     *
     * @param value the string value to parse
     *
     * @return the parsed value
     *
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    @SuppressWarnings("unchecked")
    public T parseValue(@NotNull String value) throws IllegalArgumentException {
        try {
            if (type == Boolean.class) {
                return (T) Boolean.valueOf(value);
            } else if (type == Integer.class) {
                return (T) Integer.valueOf(value);
            } else if (type == Double.class) {
                return (T) Double.valueOf(value);
            } else if (type == String.class) {
                return (T) value;
            } else if (type == Long.class) {
                return (T) Long.valueOf(value);
            } else if (type == Float.class) {
                return (T) Float.valueOf(value);
            } else if (type == Location.class) {
                String[] split = value.split(",");
                if (split.length != 3) {
                    throw new IllegalArgumentException("Location must be in the format 'x,y,z'");
                }
                return (T) new Location(Bukkit.getWorlds().getFirst(), Double.parseDouble(split[0]), Double.parseDouble(split[1]), Double.parseDouble(split[2]));
            } else {
                throw new IllegalArgumentException("Unsupported type: " + type.getName());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value '" + value + "' for type " + type.getSimpleName());
        }
    }
    
    @Override
    public @NotNull String toString() {
        return "CustomGameRule{name='" + name + "', type=" + type.getSimpleName() + ", default=" + defaultValue + ", global=" + global + "}";
    }
}
