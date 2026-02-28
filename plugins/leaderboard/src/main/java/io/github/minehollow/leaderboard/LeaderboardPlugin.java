 package io.github.minehollow.leaderboard;

import io.github.minehollow.leaderboard.command.LeaderboardCommand;
import io.github.minehollow.leaderboard.hook.LeaderboardPapiHook;
import io.github.minehollow.leaderboard.listener.LeaderboardListener;
import io.github.minehollow.leaderboard.menu.LeaderboardListMenu;
import io.github.minehollow.leaderboard.storage.LeaderboardStorage;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.task.WrappedTask;
import io.github.minehollow.sdk.stats.StatService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

/**
 * Leaderboard plugin — manages stat-based leaderboards with hologram displays,
 * chat commands, and PlaceholderAPI integration.
 */
@Slf4j
public class LeaderboardPlugin extends SimplePlugin {

    @Getter
    private static LeaderboardPlugin instance;

    @Getter
    private LeaderboardManager manager;

    @Getter
    private StatService statService;

    private @Nullable WrappedTask refreshTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Initialize EntityLib if not already initialized
        if (EntityLib.getPlatform() == null) {
            SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(this);
            APIConfig settings = new APIConfig(PacketEvents.getAPI())
                .tickTickables()
                .usePlatformLogger();
            EntityLib.init(platform, settings);
        }

        // Stat service (connects to MongoDB)
        statService = new StatService();

        // Storage & Manager
        LeaderboardStorage storage = new LeaderboardStorage(this);
        manager = new LeaderboardManager(statService, storage);
        manager.loadAll();

        // Listener for hologram visibility
        registerListeners(new LeaderboardListener(manager));

        // Command
        registerCommands("leaderboard", new LeaderboardCommand(this));

        // Menu
        MenuUtil.registerMenus(new LeaderboardListMenu(manager));

        // Periodic data refresh (default: every 30 seconds)
        long refreshInterval = getConfig().getLong("refresh-interval", 30) * 20L;
        refreshTask = Tasks.runAsyncRepeating(() -> {
            try {
                manager.refreshAll();
            } catch (Exception e) {
                log.error("Failed to refresh leaderboards", e);
            }
        }, 40L, refreshInterval);

        // Show holograms to online players after a short delay (reload support)
        Tasks.runAsyncLater(() -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                manager.updateVisibility(player);
            }
        }, 20L);

        // PlaceholderAPI hook
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LeaderboardPapiHook(manager).register();
            log.info("PlaceholderAPI hook registered.");
        }

        log.info("LeaderboardPlugin enabled — {} leaderboards loaded.", manager.getAll().size());
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }

        if (manager != null) {
            manager.unloadAll();
        }

        instance = null;
    }
}

