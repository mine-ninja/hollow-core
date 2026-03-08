package io.github.minehollow.npc;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.task.WrappedTask;
import io.github.minehollow.npc.api.NpcRegistry;
import io.github.minehollow.npc.command.NpcCommand;
import io.github.minehollow.npc.config.NpcStorage;
import io.github.minehollow.npc.impl.NpcListener;
import io.github.minehollow.npc.impl.NpcPacketHandler;
import io.github.minehollow.npc.impl.NpcRegistryImpl;
import io.github.minehollow.npc.service.SkinService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.jetbrains.annotations.Nullable;

/**
 * Main NPC plugin class. Initializes EntityLib, loads NPCs from storage, and registers the packet handler + visibility listener.
 */
@Slf4j
public class NpcPlugin extends SimplePlugin {

    @Getter
    private static NpcPlugin instance;

    @Getter
    private NpcRegistry registry;

    @Getter
    private SkinService skinService;

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

        // Skin service
        skinService = new SkinService();

        // Config
        double renderDistance = getConfig().getDouble("render-distance", 48);
        int autoSaveInterval = getConfig().getInt("auto-save-interval", 300);

        // Storage & Registry
        NpcStorage storage = new NpcStorage(this);
        NpcRegistryImpl registryImpl = new NpcRegistryImpl(storage, renderDistance);
        registryImpl.loadAll();
        this.registry = registryImpl;

        // Packet handler (click detection)
        NpcPacketHandler packetHandler = new NpcPacketHandler(registryImpl);
        PacketEvents.getAPI().getEventManager().registerListener(packetHandler);

        // Visibility listener
        NpcListener listener = new NpcListener(registryImpl);
        registerListeners(listener);
        listener.showToOnlinePlayers();

        // Command
        registerCommands("npc", new NpcCommand(this));

        Tasks.runAsyncRepeating(registryImpl::tickAll, 5, 5);

        // Auto-save task
        if (autoSaveInterval > 0) {
            long ticks = autoSaveInterval * 20L;
            autoSaveTask = Tasks.runAsyncRepeating(registryImpl::saveIfDirty, ticks, ticks);
        }

        log.info("NpcPlugin enabled — {} NPCs loaded.", registryImpl.getAll().size());
    }

    @Override
    public void onDisable() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }

        if (registry instanceof NpcRegistryImpl impl) {
            impl.unloadAll();
        }

        instance = null;
    }
}

