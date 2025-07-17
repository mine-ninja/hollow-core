package net.warcane.lugin.core.minecraft;

import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.plugin.SimplePlugin;
import net.warcane.lugin.core.server.GameServer;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BukkitPlatformPlugin extends SimplePlugin {



    private BukkitPlatform bukkitPlatform;

    @Override
    public void onEnable() {
        bukkitPlatform = BukkitPlatform.provide(this);
        bukkitPlatform.init();

        registerCommands("lugin", new ListServersCommand());


        if (bukkitPlatform.getServerCategoryType() == ServerCategoryType.LOGIN) {
            Bukkit.getWorlds().forEach(world -> {
                world.setDifficulty(Difficulty.PEACEFUL);
                world.setTime(0);
                world.setGameRuleValue("doWeatherCycle", "false");
                world.setGameRuleValue("doDaylightCycle", "false");
                world.setGameRuleValue("doMobSpawning", "false");
            });

            Bukkit.getPluginManager().registerEvents(new LimboListener(), this);
        }
    }

    final class ListServersCommand extends SimpleCommand {
        public ListServersCommand() {
            super("listservers");
            this.requiredPermission = "lugin.manager";
        }

        @Override
        public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
            List<GameServer> gameServers = bukkitPlatform.getGameServerService().queryAllServersInNetwork();
            if (gameServers.isEmpty()) {
                ctx.getSender().sendMessage("§cNenhum servidor encontrado.");
                return;
            }

            final int size = gameServers.size();
            final var message = new StringBuilder("§aServidores (§7").append(size).append("): ");
            for (int i = 0; i < gameServers.size(); i++) {
                GameServer server = gameServers.get(i);
                final var playerCount = server.serverPlayerCount().toFormattedString();
                final var serverCategory = server.categoryType().name();
                message.append("§a").append(server.serverId())
                  .append(" §7[").append(playerCount).append("|").append(serverCategory).append("] §b")
                  .append(server.hostAddress().toString());
                if (i < gameServers.size() - 1) {
                    message.append(" §7| ");
                }
            }

            ctx.sendMessage(message.toString());
        }
    }

    public static class LimboListener implements Listener {
        @EventHandler
        public void onJoin(PlayerJoinEvent event) {
            event.setJoinMessage(null);

            final var player = event.getPlayer();
            player.setWalkSpeed(0);
        }

        @EventHandler
        public void onQuit(PlayerQuitEvent event) {
            event.setQuitMessage(null);
        }

        @EventHandler
        public void foodLevelChange(org.bukkit.event.entity.FoodLevelChangeEvent event) {
            event.setCancelled(true);
        }

        @EventHandler
        public void onDamage(org.bukkit.event.entity.EntityDamageEvent event) {
            if (event.getEntity() instanceof org.bukkit.entity.Player) {
                event.setCancelled(true);
            }
        }
    }
}
