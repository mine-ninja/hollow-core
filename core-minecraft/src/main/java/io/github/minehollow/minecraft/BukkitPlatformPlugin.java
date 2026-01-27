package io.github.minehollow.minecraft;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.EventManager;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.common.collect.Lists;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import io.github.minehollow.minecraft.centralcart.CentralCart;
import io.github.minehollow.minecraft.centralcart.listener.MapOrderListener;
import io.github.minehollow.minecraft.centralcart.listener.MapWatcher;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.compat.PAPICompat;
import io.github.minehollow.minecraft.compat.VaultCompat;
import io.github.minehollow.minecraft.menu.input.SignInputListener;
import io.github.minehollow.minecraft.placeholder.LuginPapiExpansion;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.sdk.server.GameServer;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BukkitPlatformPlugin extends SimplePlugin {
    @Getter
    private static BukkitPlatformPlugin instance;
    private BukkitPlatform bukkitPlatform;
    private BukkitAudiences adventure;
    private boolean debugMode = false;

    static {
        MinecraftVersion.disableBStats();
        MinecraftVersion.disableUpdateCheck();

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (instance.debugMode) {
                log.error("[UNCAUGHT] Thread={}", t.getName(), e);
            }
        });
    }

    @Override
    public void onLoad() {
        EventManager manager = PacketEvents.getAPI().getEventManager();
        manager.registerListener(new SignInputListener(), PacketListenerPriority.NORMAL);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (!NBT.preloadApi()) {
            this.getLogger().severe("Não foi possível carregar o NBT-API. O plugin será desativado.");
        }

        bukkitPlatform = BukkitPlatform.provide(this);
        bukkitPlatform.init();

        this.adventure = BukkitAudiences.create(this);

        registerCommands(
          "hollow",
          new ListServersCommand(),
          new ListPlayersCommand(),
          new ServerInfo(),
          new DebugCommand()
        );
        PluginManager manager = Bukkit.getPluginManager();

        if (manager.isPluginEnabled("PlaceholderAPI")) {
            new PAPICompat(this, bukkitPlatform.getNameTagResolver()).register();
            new LuginPapiExpansion().register();
        }
        if (manager.isPluginEnabled("Vault")) {
            VaultCompat.register(this);
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

        CentralCart centralCart = bukkitPlatform.getCentralCart();
        if (!centralCart.isTokenValid()) {
            this.getLogger().severe("O token do Central Cart não foi definido. Verifique a variável de ambiente 'CENTRAL_CART_TOKEN'. Compras In-Game estarão desabilitadas.");
        } else {
            Tasks.runAsync(centralCart::loadProducts);
            Tasks.runAsyncRepeating(new MapWatcher(), 0L, 600L); // 30 segundos

            manager.registerEvents(new MapOrderListener(this), this);
            this.getLogger().info("Central Cart habilitado com sucesso.");
        }
    }

    @Override
    public void onDisable() {
        if (bukkitPlatform != null) {
            bukkitPlatform.close();
        }
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    public @NotNull BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    final class ListServersCommand extends SimpleCommand {
        public ListServersCommand() {
            super("listservers", "hollow.manager");
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
            int totalPlayers = 0;
            for (int i = 0; i < gameServers.size(); i++) {
                GameServer server = gameServers.get(i);
                final var serverPlayers = server.serverPlayers();
                totalPlayers += serverPlayers.online();
                final var playerCount = serverPlayers.toFormattedString();
                final var serverCategory = server.categoryType().name();
                message = message.append(Component.text("§a" + server.serverId())).append(Component.text(" §7[" + playerCount + " | " + serverCategory + "]")).append(Component.text(" §b" + server.hostAddress().toString()));
                if (i < gameServers.size() - 1) {
                    message = message.append(Component.newline());
                }
            }
            message = message.append(Component.newline()).append(Component.text("§aTotal de jogadores online na rede: §b" + totalPlayers));
            ctx.sendMessage(message);
        }
    }

    final class ListPlayersCommand extends SimpleCommand {
        public ListPlayersCommand() {
            super("listplayers", "hollow.manager");
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
                List<String> list = bukkitPlatform.getGameServerService().queryAllServersInNetwork().stream().map(GameServer::serverId).collect(Collectors.toCollection(Lists::newArrayList));
                list.removeIf(s -> !s.startsWith(ctx.getRawArgOrNull(0)));
                return list;
            }
            return super.performTabComplete(ctx);
        }
    }

    final class ServerInfo extends SimpleCommand {
        public ServerInfo() {
            super("serverinfo", "hollow.manager");
        }

        @Override
        public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
            final var server = bukkitPlatform.getGameServer();
            final var playerCount = server.serverPlayers().toFormattedString();
            ctx.sendMessage("§aServidor: §b" + server.serverId() + " §7[" + playerCount + "] §b" + " " + server.categoryType().name() + " | " + server.subCategory().name() + " | " + server.hostAddress().toString());
        }
    }

    final class DebugCommand extends SimpleCommand {
        public DebugCommand() {
            super("debug", "hollow.manager");
        }

        @Override
        public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
            var cmd = ctx.getRawArgOrThrow(0, "§cUso: /debug <log-uncaught>");
            if (cmd.equalsIgnoreCase("log-uncaught")) {
                debugMode = !debugMode;
                ctx.getSender().sendMessage("§aModo de debug de exceções não capturadas " + (debugMode ? "ativado" : "desativado") + ".");
            } else {
                throw new CommandFailedException("§cUso: /debug <log-uncaught>");
            }
        }

        @Override
        public List<String> performTabComplete(@NotNull CommandContext ctx) {
            return List.of("log-uncaught");
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
        public void foodLevelChange(FoodLevelChangeEvent event) {
            event.setCancelled(true);
        }

        @EventHandler
        public void onDamage(EntityDamageEvent event) {
            if (event.getEntity() instanceof Player) {
                event.setCancelled(true);
            }
        }
    }
}
