package io.github.minehollow.hologram;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.minehollow.hologram.api.HologramRegistry;
import io.github.minehollow.hologram.command.HologramCommand;
import io.github.minehollow.hologram.impl.HologramListener;
import io.github.minehollow.hologram.impl.HologramRegistryImpl;
import io.github.minehollow.hologram.impl.HologramStorage;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.task.WrappedTask;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.jetbrains.annotations.Nullable;

/**
 * Main Hologram plugin class. Initializes EntityLib, loads holograms from
 * storage, and registers the visibility listener + admin command.
 * <p>
 * Other plugins can access the hologram API via:
 * <pre>{@code
 *   HologramPlugin.getInstance().getRegistry();
 * }</pre>
 */
@Slf4j
public class HologramPlugin extends SimplePlugin {

    @Getter
    private static HologramPlugin instance;

    @Getter
    private HologramRegistry registry;

    private @Nullable WrappedTask autoSaveTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Initialize EntityLib if not already initialized by another plugin
        if (EntityLib.getPlatform() == null) {
            SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(this);
            APIConfig settings = new APIConfig(PacketEvents.getAPI())
                    .tickTickables()
                    .usePlatformLogger();

            EntityLib.init(platform, settings);
        }

        // Config
        double renderDistance = getConfig().getDouble("render-distance", 48);
        int autoSaveInterval = getConfig().getInt("auto-save-interval", 300);

        // Storage & Registry
        HologramStorage storage = new HologramStorage(this);
        HologramRegistryImpl registryImpl = new HologramRegistryImpl(storage, renderDistance);
        registryImpl.loadAll();
        this.registry = registryImpl;

        // Visibility listener
        HologramListener listener = new HologramListener(registryImpl);
        registerListeners(listener);
        listener.showToOnlinePlayers();

        // Command
        registerCommands("hologram", new HologramCommand(this));

        // Auto-save task
        if (autoSaveInterval > 0) {
            long ticks = autoSaveInterval * 20L;
            autoSaveTask = Tasks.runAsyncRepeating(registryImpl::saveIfDirty, ticks, ticks);
        }

        log.info("HologramPlugin enabled — {} holograms loaded.", registryImpl.getAll().size());
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        if (registry instanceof HologramRegistryImpl impl) {
            impl.unloadAll();
        }

        instance = null;
        log.info("HologramPlugin disabled.");
    }
}
