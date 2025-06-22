package net.warcane.lugin.core.minecraft.npc;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import lombok.Getter;
import net.warcane.lugin.core.minecraft.npc.listener.NpcPacketListener;
import net.warcane.lugin.core.minecraft.npc.listener.NpcRenderListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Getter
public class NpcManager {

    private final Plugin plugin;
    private final TIntObjectMap<Npc> cachedNpcMap = new TIntObjectHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("npc-core-thread")
        .setPriority(Thread.MIN_PRIORITY)
        .build()
    );

    public NpcManager(Plugin plugin) {
        this.plugin = plugin;
        this.initalize();
    }

    private void initalize() {
        var npcListener = new NpcRenderListener(this);

        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(npcListener, plugin);
        pluginManager.registerEvents(new NpcPacketListener(this), plugin);

        executorService.scheduleWithFixedDelay(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                npcListener.updatePlayer(player);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }


    @NotNull
    public Npc createNpc(@NotNull Location location) {
        Npc npc = new Npc(location);
        cachedNpcMap.put(npc.getEntityId(), npc);
        return npc;
    }

    @Nullable
    public Npc getNpc(int npcId) {
        return cachedNpcMap.get(npcId);
    }

    public boolean hasNpc(int npcId) {
        return cachedNpcMap.containsKey(npcId);
    }

    public void unloadNpc(@NotNull Npc npc) {
        cachedNpcMap.remove(npc.getEntityId());
        npc.getVisibilityHandler().hideToAll();
    }
}
