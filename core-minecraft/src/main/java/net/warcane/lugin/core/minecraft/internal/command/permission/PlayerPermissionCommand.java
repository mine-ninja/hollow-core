package net.warcane.lugin.core.minecraft.internal.command.permission;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerLosePermissionPacket;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerReceivePermissionPacket;
import net.warcane.lugin.core.player.account.PlayerAccountService;
import net.warcane.lugin.core.util.time.Time;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;

@Slf4j
public class PlayerPermissionCommand extends SimpleCommand {

    private static final List<String> PERMANENT_ARG_VALUES = List.of("permanente", "perma", "-p", "-1");

    @NotNull
    private final BukkitPlatform platform;
    private final PlayerAccountService playerAccountService;

    public PlayerPermissionCommand(@NotNull BukkitPlatform platform) {
        super("playerpermission", "lugin.master");
        setAliases(List.of("ppermission", "pperm"));
        this.platform = platform;
        this.playerAccountService = platform.getPlayerAccountService();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrThrow(0, "§cVocê deve especificar um subcomando: add, remove ou list.");
        final var playerName = ctx.getRawArgOrThrow(1, "§cVocê deve especificar o nome do jogador.");

        switch (subCommand.toLowerCase()) {
            case "add" -> handleAddPermissionCommand(ctx, playerName);
            case "remove" -> handleRemovePermissionCommand(ctx, playerName);
            case "list" -> handleListPermissionsCommand(ctx, playerName);
            default -> throw new CommandFailedException("§cSubcomando inválido. Use: add, remove ou list.");
        }
    }

    private void handleAddPermissionCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        var permission = ctx.getRawArgOrThrow(2, "§cVocê deve especificar a permissão do jogador");
        var rawTime = ctx.getRawArgOrThrow(3, "§cVocê deve especificar o tempo de duração do grupo (use 'permanente' para uma permissão permanente).");
        var isPermanent = PERMANENT_ARG_VALUES.contains(rawTime.toLowerCase());
        var parsedTime = isPermanent ? null : parseInstant(rawTime);

        ctx.sendMessage("§7§oProcurando jogador %s na base de dados...".formatted(playerName));

        playerAccountService.getPlayerAccountByName(playerName).whenComplete((account, error) -> {
            if (error != null) {
                error.printStackTrace();
                ctx.sendMessage("§cErro ao buscar conta do jogador: " + error.getMessage());
                return;
            }
            if (account == null) {
                ctx.sendMessage("§cJogador não encontrado: " + playerName);
                return;
            }

            log.info("Adicionando a permissão {} ao jogador {} com tempo: {}", permission, playerName, rawTime);
            ctx.sendMessage("§7§oAdicionando a permissão %s ao jogador %s...".formatted(permission, playerName));

            var updatedAccountFuture = isPermanent || parsedTime == null
                ? account.withNewPermanentPermissions(permission)
                : account.withNewPermissions(permission, parsedTime);

            playerAccountService.updatePlayerAccount(updatedAccountFuture).whenComplete((updatedAccount, updateError) -> {
                if (updateError != null) {
                    log.error("Erro ao atualizar conta do jogador {}: {}", playerName, updateError.getMessage());
                    ctx.sendMessage("§cErro ao atualizar conta do jogador: " + updateError.getMessage());
                    return;
                }

                var updatedSubscription = updatedAccount.getPermission(permission);
                if (updatedSubscription != null) {
                    final var confirmationPacket = new PlayerReceivePermissionPacket(updatedAccount.uniqueId(), permission);
                    platform.getNetworkClient().sendNetworkPacket(NetworkChannel.SERVER_STATUS, confirmationPacket);

                    ctx.sendMessage("§aPermissão %s adicionado ao jogador %s com sucesso. Expira em: %s"
                        .formatted(permission, playerName, updatedSubscription.permissionEnd()));
                } else {
                    ctx.sendMessage("§cErro a adicionar permissão ao jogador: %s.".formatted(playerName));
                }
            });
        });
    }

    private void handleRemovePermissionCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        var permission = ctx.getRawArgOrThrow(2, "§cVocê deve especificar a permissão do jogador");

        playerAccountService.getPlayerAccountByName(playerName).whenComplete((account, error) -> {
            if (error != null) throw new CommandFailedException("§cErro ao buscar conta do jogador: " + error.getMessage());
            if (account == null) throw new CommandFailedException("§cJogador não encontrado: " + playerName);

            var playerPermission = account.getPermission(permission);
            if (playerPermission == null || playerPermission.isExpired()) {
                throw new CommandFailedException("§cO jogador %s não possuie essa permissão %s.".formatted(playerName, permission));
            }

            playerAccountService.updatePlayerAccount(account.removePermissions(permission)).whenComplete((updatedAccount, updateError) -> {
                if (updateError != null)
                    throw new CommandFailedException("§cErro ao atualizar conta do jogador: " + updateError.getMessage());
                if (updatedAccount == null)
                    throw new CommandFailedException("§cErro a remover a permissão do jogador: " + playerName);

                if (updatedAccount.getPermission(permission) == null) {
                    final var confirmationPacket = new PlayerLosePermissionPacket(updatedAccount.uniqueId(), permission);
                    platform.getNetworkClient().sendNetworkPacket(NetworkChannel.SERVER_STATUS, confirmationPacket);
                }

                ctx.sendMessage("§aA permissão %s foi removido do jogador %s com sucesso.".formatted(permission, playerName));
            });
        });
    }

    private void handleListPermissionsCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        playerAccountService.getPlayerAccountByName(playerName)
            .whenComplete((account, error) -> {
                if (error != null)
                    throw new CommandFailedException("§cErro ao buscar conta do jogador: " + error.getMessage());
                if (account == null) throw new CommandFailedException("§cJogador não encontrado: " + playerName);

                var playerPermissions = account.getPermissions();
                if (playerPermissions.isEmpty()) {
                    ctx.sendMessage("§7O jogador §f%s §7não possui nenhuma permissão.".formatted(playerName));
                    return;
                }

                ctx.sendMessage("§f%s §8• §7%d permissões".formatted(playerName, playerPermissions.size()));
                ctx.sendMessage("");

                playerPermissions.forEach(permission -> {
                    var message = permission.isPermanent()
                        ? "  §a• %s §7permanente".formatted(permission.permission())
                        : "  §a• %s §7até %s".formatted(permission.permission(), permission.permissionEnd());
                    ctx.sendMessage(message);
                });

                ctx.sendMessage("");
            });
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        final var args = ctx.getArgs();

        if (args.length == 1) {
            return filterStartingWith(List.of("add", "remove", "list"), args[0]);
        }
        if (args.length == 2) {
            return filterStartingWith(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 4 && !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("remove")) {
            return filterStartingWith(PERMANENT_ARG_VALUES, args[3]);
        }

        return super.performTabComplete(ctx);
    }

    private Instant parseInstant(@NotNull String rawTime) {
        try {
            return Time.parseString(rawTime).toInstantFromNow();
        } catch (Exception e) {
            throw new CommandFailedException("§cFormato de tempo inválido: " + rawTime);
        }
    }
}
