package net.warcane.lugin.core.minecraft.internal.command.permission;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.event.PlayerAccountUpdateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
public class PlayerGroupCommand extends SimpleCommand {

    private static final List<String> PERMANENT_ARG_VALUES = List.of("permanente", "perma", "-p", "-1");

    private final BukkitPlatform platform;

    public PlayerGroupCommand(@NotNull BukkitPlatform platform) {
        super("playergroup");
        this.platform = platform;
        this.requiredPermission = "lugin.master";
    }

    // /playergroup set <player> <group> <time>
    // /playergroup remove <player>
    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrThrow(0, "Você deve especificar um subcomando. Exemplo: /playergroup set <player> <group> <time>");
        final var localPlayer = ctx.getLocalPlayerOrThrow(1, "Você deve especificar um local.");

        switch (subCommand.toLowerCase()) {
            case "set":
                this.handleSetGroupCommand(ctx, localPlayer);
                break;
            case "remove":
                this.handleRemoveGroupCommand(ctx, localPlayer);
                break;
            default:
                throw new CommandFailedException("Subcomando desconhecido. Use 'set' ou 'remove'.");
        }
    }

    private void handleSetGroupCommand(@NotNull CommandContext ctx, @NotNull Player localPlayer) {
        final var groupToSet = ctx.getEnumOrThrow(2, PlayerGroup.class,
          "Você deve especificar um grupo válido. Grupos atuais: " + String.join(", ", PlayerGroup.NAMES));

        final var cachedAccount = platform.getPlayerAccountService().getCachedAccount(localPlayer.getUniqueId());
        if (cachedAccount == null) {
            throw new CommandFailedException("Conta do jogador não encontrada. Certifique-se de que o jogador está online.");
        }

        final var rawTime = ctx.getRawArgOrThrow(3, "Você deve especificar um tempo para o grupo.");
        boolean isPermanent = PERMANENT_ARG_VALUES.contains(rawTime.toLowerCase());
        Instant expirationTime;
        if (isPermanent) {
            expirationTime = ZonedDateTime.now().plusYears(1000).toInstant();
        } else {
            final var time = ctx.getTimeOrThrow(3, "Você deve especificar um tempo válido. Exemplo: 1h, 30m, 15s.");
            expirationTime = Instant.now().plusMillis(time.toMilliseconds());
        }

        platform.getPlayerAccountService().updatePlayerAccount(
          cachedAccount.withNewGroupSubscription(groupToSet, expirationTime)
        ).whenComplete((updated, error) -> {
            if (error != null) {
                log.error("Erro ao definir grupo para o jogador {}: {}", localPlayer.getName(), error.getMessage());
                ctx.sendMessage("§cErro ao definir grupo: " + error.getMessage());
            } else if (updated != null) {
                // todo: chamar algum evento de mudança de grupo.
                final var event = new PlayerAccountUpdateEvent(updated);
                Bukkit.getPluginManager().callEvent(event);

                ctx.sendMessage("§aGrupo " + groupToSet.name() + " definido para o jogador " + localPlayer.getName() + " com sucesso.");
            } else {
                ctx.sendMessage("§cErro incomum ao definir grupo para o jogador " + localPlayer.getName() + ".");
            }
        });
    }

    private void handleRemoveGroupCommand(@NotNull CommandContext ctx, @NotNull Player localPlayer) {
        final var cachedAccount = platform.getPlayerAccountService().getCachedAccount(localPlayer.getUniqueId());
        if (cachedAccount == null) {
            throw new CommandFailedException("Conta do jogador não encontrada. Certifique-se de que o jogador está online.");
        }

        platform.getPlayerAccountService().updatePlayerAccount(
          cachedAccount.withNewGroupSubscription(PlayerGroup.DEFAULT, Instant.now().plusMillis(Instant.MAX.toEpochMilli()))
        ).whenComplete((found, error) -> {
            if (error != null) {
                log.error("Erro ao remover grupo do jogador {}: {}", localPlayer.getName(), error.getMessage());
            } else if (found != null) {

                final var event = new PlayerAccountUpdateEvent(found);
                Bukkit.getPluginManager().callEvent(event);

                ctx.sendMessage("§aGrupo removido com sucesso do jogador " + localPlayer.getName() + ".");
            } else {
                ctx.sendMessage("§cErro incomum ao remover grupo do jogador " + localPlayer.getName() + ".");
            }
        });
    }
}
