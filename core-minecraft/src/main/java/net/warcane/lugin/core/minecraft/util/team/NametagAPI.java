package net.warcane.lugin.core.minecraft.util.team;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * NametagAPI by @KAZHIEL
 * Based on code from NametagEdit with massive modifications
 */
@Getter
public class NametagAPI implements Listener {

    @Getter
    private static NametagAPI instance;

    private Plugin plugin;
    private NametagHandler handler;

    public NametagAPI(Plugin javaPlugin) {
        this.plugin = javaPlugin;
        this.handler = new NametagHandler(javaPlugin);

        this.plugin.getServer().getPluginManager().registerEvents(this, javaPlugin);
    }

    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        handler.resetPlayer(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        handler.sendTeams(event.getPlayer());
    }

    public void applyTag(Player player, NametagTeam nametagTeam) {
        new BukkitRunnable() {
            public void run() {
                handler.setNametag(player, nametagTeam.getPrefix(), nametagTeam.getSuffix(), nametagTeam.getPriority());
            }
        }.runTaskLater(plugin, 0L);
    }

    public void applyTag(Player player, String prefix, String suffix, int sortPriority) {
        new BukkitRunnable() {
            public void run() {
                handler.setNametag(player, prefix, suffix, sortPriority);
            }
        }.runTaskLater(plugin, 0L);
    }

    public void resetPlayer(Player p) {
        handler.resetPlayer(p);
    }

    public static void registerApi(Plugin javaPlugin) {
        instance = new NametagAPI(javaPlugin);
    }

}
