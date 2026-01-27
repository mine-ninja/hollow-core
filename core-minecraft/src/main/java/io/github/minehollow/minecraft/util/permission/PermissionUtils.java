package io.github.minehollow.minecraft.util.permission;

import org.bukkit.entity.Player;

public class PermissionUtils {
    /**
     * Retrieves the highest numbered permission for a player based on a given base string.
     * <p>
     * This method checks the player's effective permissions and looks for permissions
     * that start with the specified base string followed by a numeric suffix. It then
     * extracts the numeric suffix, converts it to an integer, and returns the highest value.
     * <p>
     * If the suffix is not a valid number, it is ignored (treated as 0).
     *
     * @param player The player whose permissions are being checked. If null, the method returns 0.
     * @param base   The base string to match permissions against. If null or empty, the method returns 0.
     * @return The highest numeric suffix found in the player's permissions matching the base string.
     *         Returns 0 if no matching permissions are found or if the input is invalid.
     */
    public static int getHighestNumberedPermission(Player player, String base) {
        if (player == null || base == null || base.isEmpty()) return 0;
        
        String prefix = base + ".";
        return player.getEffectivePermissions().stream()
            .filter(i -> i.getValue() && i.getPermission().startsWith(prefix))
            .mapToInt(i -> {
                try {
                    return Integer.parseInt(i.getPermission().substring(prefix.length()));
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .max().orElse(0);
    }
}
