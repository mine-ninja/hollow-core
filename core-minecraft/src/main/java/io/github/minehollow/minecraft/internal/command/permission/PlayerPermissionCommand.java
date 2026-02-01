package io.github.minehollow.minecraft.internal.command.permission;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.event.ClickEvent;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.BukkitPlatformPlugin;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.util.message.ComponentBuilder;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.permission.PlayerLosePermissionPacket;
import io.github.minehollow.sdk.network.packet.impl.player.permission.PlayerReceivePermissionPacket;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import io.github.minehollow.sdk.player.account.PlayerAccountService;
import io.github.minehollow.sdk.util.time.Time;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class PlayerPermissionCommand extends SimpleCommand {

    private static final List<String> PERMANENT_ARG_VALUES = List.of("permanente", "perma", "-p", "-1");
    private static final List<String> PERMANENT_SUBCOMMANDS_VALUES = List.of("add", "remove", "list");

    @NotNull
    private final BukkitPlatform platform;
    private final PlayerAccountService playerAccountService;

    public PlayerPermissionCommand(@NotNull BukkitPlatform platform) {
        super("playerpermission", "hollow.master");
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
        var rawTime = ctx.getRawArgOrThrow(3, "§cVocê deve especificar o tempo de duração da permissão (use 'permanente' para uma permissão permanente).");
        var isPermanent = PERMANENT_ARG_VALUES.contains(rawTime.toLowerCase());
        var parsedTime = isPermanent ? null : parseInstant(rawTime);

        ctx.sendMessage("§7§oProcurando jogador %s na base de dados...".formatted(playerName));

        playerAccountService.getPlayerAccountByName(playerName).whenComplete((account, error) -> {
            if (handleError(ctx, playerName, account, error)) return;

            log.info("Adicionando a permissão {} ao jogador {} com tempo: {}", permission, playerName, rawTime);
            ctx.sendMessage("§7§oAdicionando a permissão %s ao jogador %s...".formatted(permission, playerName));

            var updatedAccountFuture = isPermanent || parsedTime == null
                ? account.withNewPermanentPermissions(permission)
                : account.withNewPermissions(permission, parsedTime);

            playerAccountService.updatePlayerAccount(updatedAccountFuture).whenComplete((updatedAccount, updateError) -> {
                if (handleUpdateError(ctx, playerName, updateError)) return;

                var updatedPermission = updatedAccount.getPermission(permission);
                if (updatedPermission != null) {
                    var targetPlayer = Bukkit.getPlayer(playerName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        platform.getPermissionInjector().injectPermissions(targetPlayer);
                    } else {
                        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.SERVER_STATUS, new PlayerReceivePermissionPacket(updatedAccount.uniqueId(), permission));
                    }

                    ctx.sendMessage("§aPermissão %s adicionado ao jogador %s com sucesso. Expira em: %s"
                        .formatted(permission, playerName, updatedPermission.permissionEnd(), DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")));
                } else {
                    ctx.sendMessage("§cErro a adicionar permissão ao jogador: %s.".formatted(playerName));
                }
            });
        });
    }

    private void handleRemovePermissionCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        var permission = ctx.getRawArgOrThrow(2, "§cVocê deve especificar a permissão do jogador");

        playerAccountService.getPlayerAccountByName(playerName).whenComplete((account, error) -> {
            if (handleError(ctx, playerName, account, error)) return;

            var playerPermission = account.getPermission(permission);
            if (playerPermission == null || playerPermission.isExpired()) {
                throw new CommandFailedException("§cO jogador %s não possuie essa permissão %s.".formatted(playerName, permission));
            }

            var updatedAccountFuture = account.removePermissions(permission);
            playerAccountService.updatePlayerAccount(updatedAccountFuture).whenComplete((updatedAccount, updateError) -> {
                if (handleUpdateError(ctx, playerName, updateError)) return;

                if (updatedAccount.getPermission(permission) == null) {
                    var targetPlayer = Bukkit.getPlayer(playerName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        platform.getPermissionInjector().injectPermissions(targetPlayer);
                    } else {
                        platform.getNetworkClient().sendNetworkPacket(
                            NetworkChannel.SERVER_STATUS,
                            new PlayerLosePermissionPacket(updatedAccount.uniqueId(), permission)
                        );
                    }
                }

                ctx.sendMessage("§aA permissão %s foi removido do jogador %s com sucesso.".formatted(permission, playerName));
            });
        });
    }

    private void handleListPermissionsCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        playerAccountService.getPlayerAccountByName(playerName).whenComplete((account, error) -> {
            if (handleError(ctx, playerName, account, error)) return;

            var playerPermissions = account.getPermissions();
            if (playerPermissions.isEmpty()) {
                ctx.sendMessage("§7O jogador §f%s §7não possui nenhuma permissão.".formatted(playerName));
                return;
            }

            var audience = BukkitPlatformPlugin.getInstance().adventure().player(ctx.getSenderAsPlayer());
            var msg = new ComponentBuilder()
                .newLine()
                .simple("<l-green>Permissões de <l-white>" + playerName + " (" + playerPermissions.size() + ")" + "<l-yellow>:").newLine()
                .newLine();

            playerPermissions.forEach(permission -> {
                msg.simple("  <l-gray>• " + permission.permission() + (permission.isPermanent() ? " - permanente" : " até " + DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(permission.permissionEnd())))
                    .actionHover(" <l-red>[✗]", ClickEvent.runCommand("/pperm remove " + playerName + " " + permission.permission()), "<l-gray>Clique para remover essa permissão.")
                    .newLine();
            });

            msg.send(audience);
        });
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        final var args = ctx.getArgs();

        if (args.length == 1) {
            return filterStartingWith(PERMANENT_SUBCOMMANDS_VALUES, args[0]);
        }
        if (args.length == 2) {
            return filterStartingWith(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 4 && !args[0].equalsIgnoreCase("list") && !args[0].equalsIgnoreCase("remove")) {
            return filterStartingWith(PERMANENT_ARG_VALUES, args[3]);
        }

        return super.performTabComplete(ctx);
    }

    private boolean handleError(@NotNull CommandContext ctx, @NotNull String playerName, @Nullable PlayerAccount account, @UnknownNullability Throwable error) {
        if (error != null) {
            log.error("Erro ao buscar conta do jogador {}: {}", playerName, error.getMessage());
            ctx.sendMessage("§cErro ao buscar conta do jogador: " + error.getMessage());
            return true;
        }
        if (account == null) {
            ctx.sendMessage("§cJogador não encontrado: " + playerName);
            return true;
        }
        return false;
    }

    private boolean handleUpdateError(@NotNull CommandContext ctx, @NotNull String playerName, @UnknownNullability Throwable error) {
        if (error != null) {
            log.error("Erro ao atualizar conta do jogador {}: {}", playerName, error.getMessage());
            ctx.sendMessage("§cErro ao atualizar conta do jogador: " + error.getMessage());
            return true;
        }
        return false;
    }

    private Instant parseInstant(@NotNull String rawTime) {
        try {
            return Time.parseString(rawTime).toInstantFromNow();
        } catch (Exception e) {
            throw new CommandFailedException("§cFormato de tempo inválido: " + rawTime);
        }
    }
}
