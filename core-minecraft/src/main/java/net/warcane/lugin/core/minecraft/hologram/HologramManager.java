package net.warcane.lugin.core.minecraft.hologram;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import net.warcane.lugin.core.minecraft.hologram.listener.HologramRenderListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class HologramManager {

    private final Plugin plugin;
    private final Map<UUID, Hologram> cachedHologramMap = new HashMap<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("hologram-core-thread")
        .setPriority(Thread.MIN_PRIORITY)
        .build()
    );

    public HologramManager(Plugin plugin) {
        this.plugin = plugin;
        init();
    }

    public void init() {
        HologramRenderListener listener = new HologramRenderListener(this);
        Bukkit.getPluginManager().registerEvents(listener, plugin);


        executorService.scheduleWithFixedDelay(() -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                listener.updateAllHologramsForPlayer(player, true);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    @NotNull
    public Hologram createHologram(@NotNull Location location) {
        Hologram hologram = new Hologram(location);
        cachedHologramMap.put(hologram.getUniqueId(), hologram);
        return hologram;
    }

    @NotNull
    public Hologram getHologram(@NotNull UUID hologramId) {
        return cachedHologramMap.get(hologramId);
    }

    public void removeHologram(@NotNull UUID hologramId) {
        Hologram removed = cachedHologramMap.remove(hologramId);
        if (removed != null) {
            removed.hideToAll();
        }
    }
}
