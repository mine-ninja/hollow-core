package io.github.minehollow.minecraft.nametag;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
public class NameTagHandler {

    private final BukkitPlatform platform;

    private final Object2LongMap<UUID> lastUpdateMap = new Object2LongOpenHashMap<>();
    private final Object2ObjectMap<UUID, CachedTagData> tagCache = new Object2ObjectOpenHashMap<>();
    private TabAPI cachedTabAPI;

    /**
     * Classe interna para armazenar dados da tag em cache
     */
    @Data
    private static final class CachedTagData {
        final String prefix;
        final String suffix;
        final String teamName;
        final int groupId;
    }

    public void setLastUpdateForPlayer(@NotNull Player player, long timestamp) {
        lastUpdateMap.put(player.getUniqueId(), timestamp);
    }

    public void clearCacheForPlayer(@NotNull Player player) {
        final UUID playerId = player.getUniqueId();
        lastUpdateMap.removeLong(playerId);
        tagCache.remove(playerId);
    }

    private boolean isInCooldown(@NotNull UUID playerId, long cooldownMillis) {
        final long lastUpdate = lastUpdateMap.getOrDefault(playerId, 0L);
        return (System.currentTimeMillis() - lastUpdate) < cooldownMillis;
    }

    private TabAPI getTabAPI() {
        if (cachedTabAPI == null) {
            cachedTabAPI = TabAPI.getInstance();
        }
        return cachedTabAPI;
    }

    public void updateAll() {
        final TabAPI tabAPI = getTabAPI();
        if (tabAPI == null) return;

        for (PlayerAccount cachedAccount : platform.getPlayerAccountService().getCachedAccounts()) {
            final Player player = Bukkit.getPlayer(cachedAccount.uniqueId());
            if (player != null && player.isOnline()) {
                updateTagForPlayer(player, false);
            }
        }
    }


    public void updateTagForPlayer(@NotNull Player player) {
        updateTagForPlayer(player, false);
    }

    public void forceUpdateTagForPlayer(@NotNull Player player) {
        updateTagForPlayer(player, true);
    }


    private void updateTagForPlayer(@NotNull Player player, boolean force) {
        final UUID playerId = player.getUniqueId();
        if (!force && isInCooldown(playerId, 1000L)) {
            return;
        }

        final PlayerAccount cachedAccount = platform.getPlayerAccountService().getCachedAccount(playerId);
        if (cachedAccount == null) {
            return;
        }

        final TabAPI tabAPI = getTabAPI();
        if (tabAPI == null) {
            return;
        }

        final TabPlayer tabPlayer = tabAPI.getPlayer(playerId);
        if (tabPlayer == null) {
            return;
        }

        final var resolver = platform.getNameTagResolver();
        final var group = cachedAccount.getHighestSubscription().group();
        final int groupId = group.getPriorityValue();
        final String teamName = "HL-" + groupId;
        final String newPrefix = resolver.getTagPrefix(player);
        final String newSuffix = resolver.getTagSuffix(player);

        final CachedTagData newData = new CachedTagData(newPrefix, newSuffix, teamName, groupId);
        final CachedTagData oldData = tagCache.get(playerId);

        if (!force && newData.equals(oldData)) {
            return;
        }

        lastUpdateMap.put(playerId, System.currentTimeMillis());
        if (oldData == null || !teamName.equals(oldData.teamName)) {
            final var sortingManager = tabAPI.getSortingManager();
            if (sortingManager != null) {
                sortingManager.forceTeamName(tabPlayer, teamName);
            }
        }

        final boolean prefixChanged = oldData == null || !newPrefix.equals(oldData.prefix);
        final boolean suffixChanged = oldData == null || !newSuffix.equals(oldData.suffix);

        if (prefixChanged || suffixChanged) {
            final var nameTagManager = tabAPI.getNameTagManager();
            if (nameTagManager != null) {
                if (prefixChanged) {
                    nameTagManager.setPrefix(tabPlayer, newPrefix);
                }
                if (suffixChanged) {
                    nameTagManager.setSuffix(tabPlayer, newSuffix);
                }
            }

            final var tabListFormatManager = tabAPI.getTabListFormatManager();
            if (tabListFormatManager != null) {
                if (prefixChanged) {
                    tabListFormatManager.setPrefix(tabPlayer, newPrefix);
                }
                if (suffixChanged) {
                    tabListFormatManager.setSuffix(tabPlayer, newSuffix);
                }
            }
        }

        tagCache.put(playerId, newData);
    }

    public void invalidateCache(@NotNull Player player) {
        tagCache.remove(player.getUniqueId());
    }

    public void clearAllCaches() {
        lastUpdateMap.clear();
        tagCache.clear();
        cachedTabAPI = null;
    }
}