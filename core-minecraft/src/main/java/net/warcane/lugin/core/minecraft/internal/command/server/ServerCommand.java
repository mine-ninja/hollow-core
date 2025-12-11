package net.warcane.lugin.core.minecraft.internal.command.server;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.player.state.PlayerNetworkState;
import net.warcane.lugin.core.player.state.PlayerNetworkStateManager;
import net.warcane.lugin.core.server.GameServer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class ServerCommand extends SimpleCommand {

    private final BukkitPlatform platform;

    public ServerCommand(BukkitPlatform platform) {
        super("server");
        this.platform = platform;
        setRequiredPermission("lugin.command.server");
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();

        if (ctx.isArgsLength(0)) {
            sendUsage(player);
            return;
        }

        var subCommand = ctx.getRawArgOrThrow(0, "§cVocê deve especificar um subcomando.");
        switch (subCommand) {
            case "join" -> handleJoinCommand(ctx, player);
            case "send" -> handleSendCommand(ctx, player);
            default -> sendUsage(player);
        }
    }

    private void sendUsage(@NotNull Player player) {
        StringUtils.send(player, "§eComando de servidor:");
        StringUtils.send(player, "§e/server join <server_id> §7- §fConecta você ao servidor especificado.");
        StringUtils.send(player, "§e/server send <player_name> <server_id>§7- §fEnvia o jogador especificado para o servidor.");
    }

    private void handleJoinCommand(@NotNull CommandContext ctx, @NotNull Player player) throws CommandFailedException {
        var serverId = ctx.getRawArgOrThrow(1, "§cVocê deve especificar o ID do servidor para o qual deseja se conectar.");
        var server = getServerOrThrow(serverId);

        platform.tryConnectPlayerToServer(player.getUniqueId(), server.serverId());
    }

    private void handleSendCommand(@NotNull CommandContext ctx, @NotNull Player player) throws CommandFailedException {
        var targetPlayerName = ctx.getRawArgOrThrow(1, "§cVocê deve especificar o nome do jogador que deseja enviar.");
        var serverId = ctx.getRawArgOrThrow(2, "§cVocê deve especificar o ID do servidor para o qual deseja enviar o jogador.");

        var server = getServerOrThrow(serverId);
        var targetState = PlayerNetworkStateManager.getInstance().getFromName(targetPlayerName);

        if (targetState == null) {
            throw new CommandFailedException("§cJogador não encontrado.");
        }

        platform.tryConnectPlayerToServer(targetState.playerId(), server.serverId());
        StringUtils.send(player, "§aJogador " + targetPlayerName + " enviado para o servidor " + serverId + ".");
    }

    private GameServer getServerOrThrow(String serverId) throws CommandFailedException {
        var server = platform.getGameServerService().getById(serverId);
        if (server == null) {
            throw new CommandFailedException("§cServidor não encontrado.");
        }
        return server;
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.isArgsLength(1)) {
            return List.of("join", "send");
        }

        var sub = ctx.getRawArgOrNull(0);

        if ("send".equalsIgnoreCase(sub)) {
            if (ctx.isArgsLength(2)) {
                return PlayerNetworkStateManager.getInstance()
                    .getOnlinePlayers()
                    .stream()
                    .filter(Objects::nonNull)
                    .map(PlayerNetworkState::playerName)
                    .toList();
            }

            if (ctx.isArgsLength(3)) {
                String arg = ctx.getRawArgOrNull(2);
                return platform.getGameServerService().queryAllServersInNetwork().stream()
                    .map(GameServer::serverId)
                    .filter(s -> s.startsWith(arg))
                    .toList();
            }
        }

        if (ctx.isArgsLength(2) && "join".equalsIgnoreCase(sub)) {
            String arg = ctx.getRawArgOrNull(1);
            return platform.getGameServerService().queryAllServersInNetwork().stream()
                .map(GameServer::serverId)
                .filter(s -> s.startsWith(arg))
                .toList();
        }

        return List.of();
    }
}
