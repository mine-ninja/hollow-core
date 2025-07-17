package net.warcane.lugin.core.minecraft.internal.command.permission;

import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
import net.warcane.lugin.core.util.time.DateFormatter;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
public class PlayerGroupCommand extends SimpleCommand {

    /**
     * Aliases para indicar que o grupo é permanente (a prova de burros).
     */
    private static final List<String> PERMANENT_ARG_VALUES = List.of("permanente", "perma", "-p", "-1");

    private final BukkitPlatform platform;

    public PlayerGroupCommand(@NotNull BukkitPlatform platform) {
        super("playergroup");
        this.platform = platform;
        this.requiredPermission = "lugin.master";
    }

    // /playergroup add <player> <group> [permanente|perma|-p|-1]
    // /playergroup remove <player> <group>
    // /playergroup list [<player>]
    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var argument = ctx.getRawArgOrThrow(0, "Você deve especificar um comando válido: add, remove ou list.");
        final var playerName = ctx.getRawArgOrThrow(1, "Você deve especificar o nome do jogador.");
        switch (argument.toLowerCase()) {
            case "add" -> handleAddGroupCommand(ctx, playerName);
            case "remove" -> handleRemoveGroupCommand(ctx, playerName);
            case "list" -> handleListGroupsCommand(ctx, playerName);
            default -> ctx.sendMessage("Comando inválido. Use: add, remove ou list.");
        }
    }

    private void handleAddGroupCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        final var group = ctx.getEnumOrThrow(2, PlayerGroup.class, "§cVocê deve especificar um grupo válido.");
        final var rawTime = ctx.getRawArgOrThrow(3, "Você deve especificar um tempo para o grupo.");
        platform.getPlayerAccountService().getPlayerAccountByName(playerName)
          .whenComplete((account, error) -> {
              if (error != null)
                  throw new CommandFailedException("§cErro ao buscar a conta do jogador: " + error.getMessage());
              if (account == null) throw new CommandFailedException("§cConta do jogador não encontrada.");

              final var isPermanent = PERMANENT_ARG_VALUES.contains(rawTime);
              Instant expirationTime;
              if (isPermanent) {
                  expirationTime = ZonedDateTime.now().plusYears(1000).toInstant();
              } else {
                  final var time = ctx.getTimeOrThrow(3, "Você deve especificar um tempo válido. Exemplo: 1h, 30m, 15s.");
                  expirationTime = Instant.now().plusMillis(time.toMilliseconds());
              }

              ctx.sendMessage("§aAdicionando o grupo §e%s§a para o jogador §e%s§a com expiração em §e%s§a.".formatted(group.name(), playerName, DateFormatter.format(expirationTime)));
              platform.getPlayerAccountService().updatePlayerAccount(account.withNewGroupSubscription(group, expirationTime))
                .whenComplete((updatedAccount, updateError) -> {
                    if (updateError != null) {
                        updateError.printStackTrace();
                        throw new CommandFailedException("§cErro ao atualizar a conta do jogador: " + updateError.getMessage());
                    }
                    if (updatedAccount == null)
                        throw new CommandFailedException("§cErro ao atualizar a conta do jogador.");

                    // sim, isso é necessário para confirmar a consistência do dado atualizado.
                    PlayerGroupSubscription subscriptionForGroup = updatedAccount.getSubscriptionForGroup(group);
                    if (subscriptionForGroup == null)
                        throw new CommandFailedException("§cErro ao adicionar o grupo: " + group.name() + " para o jogador: " + playerName);

                    final var formattedExpiration = DateFormatter.format(subscriptionForGroup.subscriptionEnd());
                    ctx.sendMessage("§aGrupo §e%s§a adicionado com sucesso para o jogador §e%s§a. Expiração: §e%s§a.".formatted(group.name(), playerName, formattedExpiration));
                });
          });
    }

    private void handleRemoveGroupCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        final var group = ctx.getEnumOrThrow(2, PlayerGroup.class, "§cVocê deve especificar um grupo válido.");

        if (group == PlayerGroup.DEFAULT) {
            throw new CommandFailedException("§cVocê não pode remover o grupo padrão (DEFAULT) de um jogador.");
        }

        platform.getPlayerAccountService().getPlayerAccountByName(playerName)
          .whenComplete((account, error) -> {
              if (error != null)
                  throw new CommandFailedException("§cErro ao buscar a conta do jogador: " + error.getMessage());
              if (account == null) throw new CommandFailedException("§cConta do jogador não encontrada.");

              final var subscription = account.getSubscriptionForGroup(group);
              if (subscription == null) {
                  ctx.sendMessage("§cO jogador §e%s§c não possui o grupo §e%s§c.".formatted(playerName, group.name()));
                  return;
              }

              ctx.sendMessage("§aRemovendo o grupo §e%s§a do jogador §e%s§a.".formatted(group.name(), playerName));
              platform.getPlayerAccountService().updatePlayerAccount(account.withNewGroupSubscription(PlayerGroup.DEFAULT, Instant.now()))
                .whenComplete((updatedAccount, updateError) -> {
                    if (updateError != null)
                        throw new CommandFailedException("§cErro ao atualizar a conta do jogador: " + updateError.getMessage());
                    if (updatedAccount == null)
                        throw new CommandFailedException("§cErro ao atualizar a conta do jogador.");

                    final var stillHasGroup = updatedAccount.getSubscriptionForGroup(group) != null;
                    if (stillHasGroup) {
                        throw new CommandFailedException("§cErro ao remover o grupo: " + group.name() + " do jogador: " + playerName);
                    }



                    ctx.sendMessage("§aGrupo §e%s§a removido com sucesso do jogador §e%s§a.".formatted(group.name(), playerName));
                });
          });
    }

    private void handleListGroupsCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        platform.getPlayerAccountService().getPlayerAccountByName(playerName)
          .whenComplete((account, error) -> {
              if (error != null)
                  throw new CommandFailedException("§cErro ao buscar a conta do jogador: " + error.getMessage());
              if (account == null) throw new CommandFailedException("§cConta do jogador não encontrada.");

              final var subscriptions = account.getSubscriptions();
              if (subscriptions.isEmpty()) {
                  ctx.sendMessage("§cO jogador §e%s§c não possui grupos.".formatted(playerName));
                  return;
              }

              ctx.sendMessage("§aGrupos do jogador §e%s§a:".formatted(playerName));
              ctx.sendMessage("§aMaior Grupo: " + account.getHighestSubscription().group().name());
              subscriptions.forEach(subscription -> {
                  final var group = subscription.group();
                  final var expiration = subscription.isPermanent()
                    ? "§ePermanente§a"
                    : "§e%s§a".formatted(DateFormatter.format(subscription.subscriptionEnd()));

                  ctx.sendMessage("§b- §e%s§a (Expiração: %s)".formatted(group.name(), expiration));
              });
          });
    }
}
