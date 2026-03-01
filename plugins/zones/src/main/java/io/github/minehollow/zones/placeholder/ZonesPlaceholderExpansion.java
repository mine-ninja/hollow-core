package io.github.minehollow.zones.placeholder;

import io.github.minehollow.zones.ZoneManager;
import io.github.minehollow.zones.ZoneQuery;
import io.github.minehollow.zones.model.Zone;
import io.github.minehollow.zones.model.ZoneFlag;
import io.github.minehollow.zones.model.ZoneFlagState;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for zones.
 * Supported placeholders:
 * <ul>
 *   <li>%zones_current_id%</li>
 *   <li>%zones_current_name%</li>
 *   <li>%zones_flag_FLAG_NAME% (e.g. %zones_flag_block-break%)</li>
 * </ul>
 */
public class ZonesPlaceholderExpansion extends PlaceholderExpansion {

    private final ZoneManager manager;
    private final ZoneQuery query;

    public ZonesPlaceholderExpansion(@NotNull ZoneManager manager, @NotNull ZoneQuery query) {
        this.manager = manager;
        this.query = query;
    }

    @Override
    public @NotNull String getIdentifier() { return "zones"; }

    @Override
    public @NotNull String getAuthor() { return "sasuked"; }

    @Override
    public @NotNull String getVersion() { return "1.0.0"; }

    @Override
    public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return null;

        if (params.equalsIgnoreCase("current_id")) {
            Zone zone = manager.getHighestPriorityZone(player.getLocation());
            return zone != null ? zone.getId() : "none";
        }

        if (params.equalsIgnoreCase("current_name")) {
            Zone zone = manager.getHighestPriorityZone(player.getLocation());
            return zone != null ? zone.getDisplayName() : "Wilderness";
        }

        if (params.startsWith("flag_")) {
            String flagKey = params.substring(5);
            ZoneFlag flag = ZoneFlag.fromKey(flagKey);
            if (flag == null) return "unknown_flag";
            ZoneFlagState state = query.resolve(player.getLocation(), flag, player.getUniqueId());
            return state.name().toLowerCase();
        }

        return null;
    }
}

