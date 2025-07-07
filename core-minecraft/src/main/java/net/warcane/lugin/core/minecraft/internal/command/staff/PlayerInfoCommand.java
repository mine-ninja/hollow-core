package net.warcane.lugin.core.minecraft.internal.command.staff;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayerInfoCommand extends SimpleCommand {

    private final BukkitPlatform platform;

    public PlayerInfoCommand(BukkitPlatform platform) {
        super("playerinfo");
        this.platform = platform;
        this.requiredPermission = "lugin.staff";
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var target = ctx.getLocalPlayerOrThrow(0, "§cInforme o nome do jogador.");
        if (target == null) {
            throw new CommandFailedException("§cJogador não encontrado no servidor no qual você está.");
        }

        final var targetAccount = platform.getPlayerAccountService().getCachedAccount(target.getUniqueId());
        if (targetAccount == null) {
            throw new CommandFailedException("§cConta do jogador não encontrada.");
        }

        ctx.sendMessage(
          "§6§lINFORMAÇÕES AVANÇADAS: §6" + target.getName(),
          "§7UUID: §f" + target.getUniqueId(),
          "§7IP: §f" + target.getAddress().getAddress().getHostAddress(),
          "§7Nome Formatado:" + targetAccount.getFormattedDisplayName(),
          "§7Ultima Conexão: §f" + targetAccount.lastLogin().toString(),
          "§7Primeira Conexão: §f" + targetAccount.createdAt().toString()
        );
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        return Bukkit.getOnlinePlayers().stream()
          .map(Player::getName)
          .toList();

    }
}
