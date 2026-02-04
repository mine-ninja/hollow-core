package io.github.minehollow.minecraft.compat;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.BukkitPlatformPlugin;
import io.github.minehollow.minecraft.nametag.resolver.NameTagResolver;
import io.github.minehollow.minecraft.util.MiniMessageColorExtractor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PAPICompat extends PlaceholderExpansion {
    private final BukkitPlatformPlugin plugin;
    private final NameTagResolver resolver;

    // Cache string -> string
    private static final Cache<String, String> PLAYER_NAME_CACHE = Caffeine.newBuilder()
      .expireAfterWrite(java.time.Duration.ofSeconds(5))
      .maximumSize(500)
      .build();

    public PAPICompat(BukkitPlatformPlugin plugin, NameTagResolver resolver) {
        this.plugin = plugin;
        this.resolver = resolver;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "hollowtags";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    // Método para versões antigas do PAPI
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        try {
            if (offlinePlayer == null || !offlinePlayer.isOnline()) {
                return null;
            }

            Player player = offlinePlayer.getPlayer();
            if (player == null) {
                return null;
            }

            return handlePlaceholder(player, params);
        } catch (Exception e) {
            throw e;
        }
    }

    // Método para versões novas do PAPI
    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        try {
            if (player == null) {
                return null;
            }

            return handlePlaceholder(player, params);
        } catch (Exception e) {
            throw e;
        }
    }

    private String handlePlaceholder(Player player, String params) {
        switch (params) {
            case "tag_priority" -> {
                final var cachedAccount = BukkitPlatform.getInstance().getPlayerAccountService().getCachedAccount(player.getUniqueId());
                if (cachedAccount == null) {
                    return String.valueOf(0);
                }

                return String.valueOf(cachedAccount.getHighestSubscription().group().getPowerLevel());
            }
            case "player_name" -> {
                final var prefix = this.resolver.getTagPrefix(player);
                final var lastColorTag = MiniMessageColorExtractor.extractLastColorTag(prefix != null ? prefix : "");

                if (lastColorTag != null) {
                    // Chave de cache = colorTag + playerName
                    final String cacheKey = lastColorTag + player.getName();

                    return PLAYER_NAME_CACHE.get(cacheKey, key -> {
                        if (lastColorTag.startsWith("<gradient:")) {
                            return lastColorTag + player.getName() + "</gradient>";
                        } else {
                            return lastColorTag + player.getName();
                        }
                    });
                } else {
                    return player.getName();
                }
            }
            case "player_prefix" -> {
                String prefix = this.resolver.getTagPrefix(player);
                String color = this.resolver.getTagColor(player);
                if (prefix != null && color != null) {
                    return prefix + color;
                }
                return prefix;
            }
            case "player_suffix" -> {
                String suffix = this.resolver.getTagSuffix(player);
                return suffix != null ? suffix : "";
            }
        }

        return null;
    }

    @Override
    public boolean register() {
        try {
            return super.register();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}