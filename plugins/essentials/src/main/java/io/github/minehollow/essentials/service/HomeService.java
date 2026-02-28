            package io.github.minehollow.essentials.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.minehollow.essentials.model.Home;
import io.github.minehollow.essentials.model.PlayerHomes;
import io.github.minehollow.essentials.repository.HomeRepository;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages player homes with an in-memory Caffeine cache backed by MongoDB.
 */
public class HomeService {

    private final HomeRepository repository;
    private final Cache<UUID, PlayerHomes> cache;

    public HomeService() {
        this.repository = new HomeRepository();
        this.cache = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(500)
            .build();
    }

    /**
     * Gets or loads a player's homes. Blocking — call from a virtual thread.
     */
    public @NotNull PlayerHomes getHomes(@NotNull UUID playerId) {
        PlayerHomes cached = cache.getIfPresent(playerId);
        if (cached != null) return cached;

        PlayerHomes loaded = repository.findByPlayer(playerId);
        if (loaded == null) {
            loaded = PlayerHomes.createEmpty(playerId);
        }
        cache.put(playerId, loaded);
        return loaded;
    }

    /**
     * Gets a specific home by name. Blocking.
     */
    public @Nullable Home getHome(@NotNull UUID playerId, @NotNull String name) {
        return getHomes(playerId).getHome(name);
    }

    /**
     * Gets all home names for a player. Blocking.
     */
    public @NotNull List<String> getHomeNames(@NotNull UUID playerId) {
        return getHomes(playerId).getHomeNames();
    }

    /**
     * Sets a home. Returns false if the limit has been reached and the home doesn't already exist.
     * Blocking.
     */
    public boolean setHome(@NotNull UUID playerId, @NotNull String name, @NotNull Location location, int limit) {
        PlayerHomes homes = getHomes(playerId);

        // If home already exists (overwrite), allow it
        boolean exists = homes.getHome(name) != null;
        if (!exists && homes.getHomes().size() >= limit) {
            return false;
        }

        Home home = new Home(
            name,
            location.getWorld().getName(),
            location.getX(),
            location.getY(),
            location.getZ(),
            location.getYaw(),
            location.getPitch()
        );

        homes.addHome(home);
        repository.save(homes);
        return true;
    }

    /**
     * Deletes a home by name. Returns false if not found. Blocking.
     */
    public boolean deleteHome(@NotNull UUID playerId, @NotNull String name) {
        PlayerHomes homes = getHomes(playerId);
        boolean removed = homes.removeHome(name);
        if (removed) {
            repository.save(homes);
        }
        return removed;
    }

    /**
     * Converts a Home object to a Bukkit Location.
     */
    public @Nullable Location toLocation(@NotNull Home home) {
        World world = Bukkit.getWorld(home.getWorld());
        if (world == null) return null;
        return new Location(world, home.getX(), home.getY(), home.getZ(), home.getYaw(), home.getPitch());
    }

    /**
     * Evicts a player from cache on disconnect.
     */
    public void evict(@NotNull UUID playerId) {
        cache.invalidate(playerId);
    }
}

