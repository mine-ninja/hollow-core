package net.warcane.lugin.core.minecraft.internal.command.permission;

import lombok.extern.slf4j.Slf4j;

import com.google.common.collect.Lists;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerLoseGroupPacket;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerReceiveGroupPacket;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.player.account.PlayerAccountService;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;
import net.warcane.lugin.core.util.time.Time;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class PlayerGroupCommand extends SimpleCommand {
    /**
     * Aliases para indicar que o grupo é permanente (a prova de burros).
     */
    private static final List<String> PERMANENT_ARG_VALUES = List.of("permanente", "perma", "-p", "-1");

    private final BukkitPlatform platform;
    private final PlayerAccountService playerAccountService;

    public PlayerGroupCommand(@NotNull BukkitPlatform platform) {
        super("playergroup", "lugin.master");
        this.platform = platform;
        this.playerAccountService = platform.getPlayerAccountService();
    }

    // /playergroup add <player> <group> [permanente|perma|-p|-1] [category]
    // /playergroup remove <player> <group> [category]
    // /playergroup list [<player>]
    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrThrow(0, "§cVocê deve especificar um subcomando: add, remove ou list.");
        final var playerName = ctx.getRawArgOrThrow(1, "§cVocê deve especificar o nome do jogador.");

        switch (subCommand.toLowerCase()) {
            case "add" -> handleAddGroupCommand(ctx, playerName);
            case "remove" -> handleRemoveGroupCommand(ctx, playerName);
            case "list" -> handleListGroupsCommand(ctx, playerName);
            default -> throw new CommandFailedException("§cSubcomando inválido. Use: add, remove ou list.");
        }
    }

    private void handleAddGroupCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        var group = ctx.getArgOrThrow(2, PlayerGroup.BY_ID::get,
          "§cGrupo inválido. Use um dos seguintes: " + String.join(", ", PlayerGroup.NAMES));

        final var rawTime = ctx.getRawArgOrThrow(3, "§cVocê deve especificar o tempo de duração do grupo (use 'permanente' para um grupo permanente).");
        final var isPermanent = PERMANENT_ARG_VALUES.contains(rawTime.toLowerCase());

        final var parsedTime = isPermanent ? null : this.parseInstant(rawTime);

        /*
          Forçamos aqui uma categoria GLOBAL, visto que as outras categorias não são mais suportadas.
         */
        var providedCategoryType = ctx.getEnumOrThrow(4, SubscriptionCategoryType.class, "§cCategoria inválida. Use uma das seguintes: " + String.join(", ", SubscriptionCategoryType.BY_NAME.keySet()));
        if (providedCategoryType != SubscriptionCategoryType.GLOBAL) {
            ctx.sendMessage("§cEsta categoria não é mais suportada. Usando GLOBAL no lugar.");
            providedCategoryType = SubscriptionCategoryType.GLOBAL;
        }

        ctx.sendMessage("§7§oProcurando jogador %s na base de dados...".formatted(playerName));


        final var categoryType = providedCategoryType;
        playerAccountService.getPlayerAccountByName(playerName)
          .whenComplete((account, error) -> {
              if (error != null) {
                  error.printStackTrace();
                  ctx.sendMessage("§cErro ao buscar conta do jogador: " + error.getMessage());
                  return;
              }

              if (account == null) {
                  ctx.sendMessage("§cJogador não encontrado: " + playerName);
                  return;
              }

              log.info("Adicionando grupo {} ao jogador {} com tempo: {} e categoria: {}", group.name(), playerName, rawTime, categoryType.name());

              ctx.sendMessage("§7§oAdicionando grupo %s ao jogador %s...".formatted(group.name(), playerName));

              playerAccountService.updatePlayerAccount(
                (isPermanent || parsedTime == null)
                  ? account.withNewPermanentSubscription(group, categoryType)
                  : account.withNewSubscription(group, parsedTime, categoryType)
              ).whenComplete((updatedAccount, updateError) -> {
                    if (updateError != null){
                        log.error("Erro ao atualizar conta do jogador {}: {}", playerName, updateError.getMessage());
                        ctx.sendMessage("§cErro ao atualizar conta do jogador: " + updateError.getMessage());
                        return;
                    }

                  final var updatedSubscription = updatedAccount.getSubscriptionForGroup(group, categoryType);
                    if (updatedSubscription != null) {
                        final var confirmationPacket = new PlayerReceiveGroupPacket(updatedAccount.uniqueId(), updatedSubscription.group(), categoryType);
                        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.SERVER_STATUS, confirmationPacket);

                        ctx.sendMessage("§aGrupo %s adicionado ao jogador %s com sucesso. Expira em: %s".formatted(group.name(), playerName, updatedSubscription.subscriptionEnd()));
                    } else {
                        ctx.sendMessage("§cErro ao adicionar grupo ao jogador: %s. Verifique se o grupo e a categoria estão corretos.".formatted(playerName));
                        ctx.sendMessage(
                          "§cGrupos do Jogador: " + updatedAccount.getSubscriptions(categoryType)
                            .stream()
                            .map(subscription -> subscription.group().getColoredDisplayName())
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("Nenhum."));
                    }
                });
          });
    }

    private void handleRemoveGroupCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        final var group = ctx.getArgOrThrow(2, PlayerGroup::fromId, "§cGrupo inválido. Use um dos seguintes: " + String.join(", ", PlayerGroup.NAMES));
        final var categoryType = ctx.getEnumOrThrow(3, SubscriptionCategoryType.class, "§cCategoria inválida. Use uma das seguintes: " + String.join(", ", SubscriptionCategoryType.BY_NAME.keySet()));

        playerAccountService.getPlayerAccountByName(playerName)
          .whenComplete((account, error) -> {
              if (error != null) throw new CommandFailedException("§cErro ao buscar conta do jogador: " + error.getMessage());
              if (account == null) throw new CommandFailedException("§cJogador não encontrado: " + playerName);

              final var subscription = account.getSubscriptionForGroup(group, categoryType);
              if (subscription == null || subscription.isExpired()) {
                  throw new CommandFailedException("§cO jogador %s não possui o grupo %s na categoria %s.".formatted(playerName, group.name(), categoryType.name()));
              }

              playerAccountService.updatePlayerAccount(account.removeSubscription(group, categoryType))
                .whenComplete((updatedAccount, updateError) -> {
                    if (updateError != null) throw new CommandFailedException("§cErro ao atualizar conta do jogador: " + updateError.getMessage());
                    if (updatedAccount == null) throw new CommandFailedException("§cErro ao remover grupo do jogador: " + playerName);

                    if (updatedAccount.getSubscriptionForGroup(group, categoryType) == null) {
                        final var confirmationPacket = new PlayerLoseGroupPacket(updatedAccount.uniqueId(), group, categoryType);
                        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.SERVER_STATUS, confirmationPacket);
                    }

                    ctx.sendMessage("§aGrupo %s removido do jogador %s com sucesso.".formatted(group.name(), playerName));
                });
          });
    }

    private void handleListGroupsCommand(@NotNull CommandContext ctx, @NotNull String playerName) {
        playerAccountService.getPlayerAccountByName(playerName)
          .whenComplete((account, error) -> {
              if (error != null)
                  throw new CommandFailedException("§cErro ao buscar conta do jogador: " + error.getMessage());
              if (account == null)
                  throw new CommandFailedException("§cJogador não encontrado: " + playerName);

              var subscriptions = account.getSubscriptions(SubscriptionCategoryType.GLOBAL);
              if (subscriptions.isEmpty()) {
                  ctx.sendMessage("§7O jogador §f%s §7não possui grupos.".formatted(playerName));
                  return;
              }

              ctx.sendMessage("§f%s §8• §7%d grupos".formatted(playerName, subscriptions.size()));
              ctx.sendMessage("");

              for (var category : SubscriptionCategoryType.BY_NAME.values()) {
                  if (category != SubscriptionCategoryType.GLOBAL) continue;

                  var categorySubscriptions = account.getSubscriptions(category);

                  if (categorySubscriptions.isEmpty()) continue;
                  var categoryName = category.getDisplayName();

                  ctx.sendMessage("§e%s §8(%d)".formatted(categoryName, categorySubscriptions.size()));

                  for (var subscription : categorySubscriptions) {
                      var group = subscription.group();
                      var isPermanent = subscription.isPermanent();

                      if (isPermanent) {
                          ctx.sendMessage("  §8• %s §7permanente".formatted(group.getColoredDisplayName()));
                      } else {
                          var endTime = subscription.subscriptionEnd();
                          ctx.sendMessage("  §8• %s §7até %s".formatted(group.getColoredDisplayName(), endTime));
                      }
                  }

                  ctx.sendMessage("");
              }
          });
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        final String[] args = ctx.getArgs();
        final int argsLength = args.length;
        
        // <add|remove|list>
        if (argsLength == 1) {
            return filterStartingWith(List.of("add", "remove", "list"), args[0]);
        }
        // <player>
        if (argsLength == 2) {
            return filterStartingWith(
                Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(),
                args[1]
            );
        }
        // <group>
        if (argsLength == 3) {
            if (args[0].equalsIgnoreCase("list")) { return NONE_ARGS; }
            return filterStartingWith(PlayerGroup.NAMES, args[2]);
        }
        // [time]/[category]
        if (argsLength == 4) {
            if (args[0].equalsIgnoreCase("list")) { return NONE_ARGS; }
            if (args[0].equalsIgnoreCase("remove")) {
                return List.of(SubscriptionCategoryType.GLOBAL.getDisplayName());
            }
            return filterStartingWith(PERMANENT_ARG_VALUES, args[3]);
        }
        if (argsLength == 5) {
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("remove")) { return NONE_ARGS; }
            return List.of(SubscriptionCategoryType.GLOBAL.getDisplayName());
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
