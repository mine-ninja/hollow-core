package net.warcane.lugin.core.minecraft;

import com.github.juliarn.npclib.api.NpcActionController;
import com.github.juliarn.npclib.api.Platform;
import com.github.juliarn.npclib.bukkit.BukkitPlatform;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class LuginCore {

    public static @NotNull Platform<World, Player, ItemStack, Plugin> getPlatform(
      @NotNull Plugin plugin,
      int spawnDistance,
      int imitateDistance,
      int tabRemovalTicks
    ) {
        return BukkitPlatform.bukkitNpcPlatformBuilder()
          .extension(plugin)
          .actionController(builder -> {
              builder.flag(NpcActionController.SPAWN_DISTANCE, spawnDistance);
              builder.flag(NpcActionController.IMITATE_DISTANCE, imitateDistance);
              builder.flag(NpcActionController.TAB_REMOVAL_TICKS, tabRemovalTicks);
          })
          .build();
    }

    public static void runSynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runAsynchronously(@NotNull Plugin plugin, @NotNull Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }
}
