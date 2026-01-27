package io.github.minehollow.minecraft.vanish;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import io.github.minehollow.sdk.util.data.RedisCache;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VanishManager {

    private static final RedisCache<String> vanishCache = new RedisCache<>(String.class);
    private static final String IDX = "vanish";

    private final Plugin plugin;

    public VanishManager(BukkitPlatform bukkitPlatform) {
        plugin = bukkitPlatform.getPlugin();
    }

    public void vanish(Player player) {
        PlayerAccount playerAccount = BukkitPlatform.getInstance().getPlayerAccountService().getCachedAccount(player.getUniqueId());

        if (playerAccount != null) {
            setVanish(player.getUniqueId(), true);

            for (Player target : Bukkit.getOnlinePlayers())
                if (!canSeeIfVanished(target))
                    target.hidePlayer(player);
        }
    }

    public void unvanish(Player player) {
        PlayerAccount playerAccount = BukkitPlatform.getInstance().getPlayerAccountService().getCachedAccount(player.getUniqueId());

        if (playerAccount != null) {
            setVanish(player.getUniqueId(), false);

            for (Player target : Bukkit.getOnlinePlayers())
                target.showPlayer(player);
        }
    }

    public boolean isVanished(Player player) {
        String value = vanishCache.get(IDX + ":" + player.getUniqueId());
        return value != null && value.equalsIgnoreCase("true");
    }

    public void setVanish(@NotNull UUID playerId, @NotNull Boolean value) {
        vanishCache.set(IDX + ":" + playerId, value.toString());
    }

    public boolean canSeeIfVanished(Player player) {
        return player.hasPermission("hollow.vanish");
    }
}
