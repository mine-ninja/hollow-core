package net.warcane.lugin.core.minecraft.whitelist;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.minecraft.BukkitPlatform;

@RequiredArgsConstructor
public class WhitelistService {

    private final BukkitPlatform whitelist;

    public boolean isWhitelistEnabled() {
        final var plugin = BukkitPlatform.getInstance().getPlugin();
        return plugin.getConfig().getBoolean("whitelist-enabled", true);
    }

    public boolean setWhitelistEnabled(boolean enabled) {
        final var plugin = BukkitPlatform.getInstance().getPlugin();
        plugin.getConfig().set("whitelist-enabled", enabled);
        plugin.saveConfig();
        return enabled;
    }

    public void setWhitelistPlayers(int count) {
        final var plugin = BukkitPlatform.getInstance().getPlugin();
        plugin.getConfig().set("whitelist-players", count);
        plugin.saveConfig();
    }

    public int getWhitelistPlayers() {
        final var plugin = BukkitPlatform.getInstance().getPlugin();
        return plugin.getConfig().getInt("whitelist-players", 0);
    }
}
