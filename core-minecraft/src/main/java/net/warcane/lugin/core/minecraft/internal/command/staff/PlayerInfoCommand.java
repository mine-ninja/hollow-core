package net.warcane.lugin.core.minecraft.internal.command.staff;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayerInfoCommand extends SimpleCommand {

    private final BukkitPlatform platform;

    public PlayerInfoCommand(BukkitPlatform platform) {
        super("playerinfo", "lugin.staff");
        this.platform = platform;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
                final var targetName = ctx.getRawArgOrThrow(0, "§cInforme o nome do jogador.");
        
        platform.getPlayerAccountService().getPlayerAccountByName(targetName)
            .whenCompleteAsync((account, throwable) -> {
                if (throwable != null) {
                    ctx.sendMessage("§cErro ao buscar a conta do jogador: " + throwable.getMessage());
                    return;
                }
                if (account == null) {
                    ctx.sendMessage("§cConta do jogador não encontrada.");
                    return;
                }
                
                final Player player = Bukkit.getPlayer(account.playerName());
                final var subscriptionType = platform.getSubscriptionCategoryType();
                ctx.sendMessage(
                    "§6§lINFORMAÇÕES AVANÇADAS: §6" + account.playerName(),
                    "§7UUID: §f" + account.uniqueId(),
                    "§7IP: §f" + (player == null ? "§7Offline ou em outro servidor." : player.getAddress().getAddress().getHostAddress()),
                    "§7Nome Formatado:" + account.getFormattedDisplayName(subscriptionType),
                    "§7Ultima Conexão: §f" + account.lastLogin().toString(),
                    "§7Primeira Conexão: §f" + account.createdAt().toString()
                );
            }, Tasks::runAsync);
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .toList();

    }
}
