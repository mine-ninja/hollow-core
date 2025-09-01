package net.warcane.lugin.core.minecraft;

import com.google.common.collect.Lists;
import net.kyori.adventure.text.Component;
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
import org.bukkit.plugin.PluginManager;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class BukkitPlatformPlugin extends SimplePlugin {

    private BukkitPlatform bukkitPlatform;

    @Override
    public void onEnable() {
        bukkitPlatform = BukkitPlatform.provide(this);
        bukkitPlatform.init();

        registerCommands("lugin", new ListServersCommand(), new ListPlayersCommand());
        PluginManager manager = Bukkit.getPluginManager();
        
        if (manager.isPluginEnabled("PlaceholderAPI")) {
            new net.warcane.lugin.core.minecraft.compat.PAPICompat(this, bukkitPlatform.getNameTagResolver()).register();
        }
        if (manager.isPluginEnabled("Vault")) {
            net.warcane.lugin.core.minecraft.compat.VaultCompat.register(this);
        }
        if (bukkitPlatform.getServerCategoryType() == ServerCategoryType.LOGIN) {
            Bukkit.getWorlds().forEach(world -> {
                world.setDifficulty(Difficulty.PEACEFUL);
                world.setTime(0);
                world.setGameRuleValue("doWeatherCycle", "false");
                world.setGameRuleValue("doDaylightCycle", "false");
                world.setGameRuleValue("doMobSpawning", "false");
            });

            manager.registerEvents(new LimboListener(), this);
        }
    }

    @Override
    public void onDisable() {
        if (bukkitPlatform != null) {
            bukkitPlatform.close();
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
            var message = Component.text("§aServidores (§7" + size + "§a): ");
            for (int i = 0; i < gameServers.size(); i++) {
                GameServer server = gameServers.get(i);

                final var playerCount = server.serverPlayers().toFormattedString();
                final var serverCategory = server.categoryType().name();

                message = message.append(Component.text("§a" + server.serverId()))
                  .append(Component.text(" §7[" + playerCount + " | " + serverCategory + "]"))
                  .append(Component.text(" §b" + server.hostAddress().toString()));
                if (i < gameServers.size() - 1) {
                    message = message.append(Component.newline());
                }
            }

            ctx.sendMessage(message);
        }
    }

    final class ListPlayersCommand extends SimpleCommand {
        public ListPlayersCommand() {
            super("listplayers");
            this.requiredPermission = "lugin.manager";
        }

        @Override
        public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
            GameServer server = bukkitPlatform.getGameServerService().getById(ctx.getRawArgOrThrow(0, "§cUso: /listplayers <serverId>"));
            if (server == null) {
                ctx.getSender().sendMessage("§cServidor não encontrado.");
                return;
            }

            final var playerCount = server.serverPlayers().toFormattedString();
            ctx.sendMessage("§aServidor: §b" + server.serverId() + " §7[" + playerCount + "|" + server.categoryType().name() + "] §b" + server.hostAddress().toString());
            final var players = server.serverPlayers();
            if (players.isEmpty()) {
                return;
            }
        }

        @Override
        public List<String> performTabComplete(@NotNull CommandContext ctx) {
            if (ctx.getArgs().length == 1) {
                List<String> list = bukkitPlatform.getGameServerService().queryAllServersInNetwork().stream()
                  .map(GameServer::serverId).collect(Collectors.toCollection(Lists::newArrayList));

                list.removeIf(s -> !s.startsWith(ctx.getRawArgOrNull(0)));

                return list;
            }
            return super.performTabComplete(ctx);
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
