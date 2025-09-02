package net.warcane.lugin.core.minecraft.internal.command.whitelist;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

public class WhiteListCommand extends SimpleCommand {

    private final BukkitPlatform platform;

    public WhiteListCommand(BukkitPlatform platform) {
        super("whitelist");
        this.platform = platform;
        this.setRequiredPermission("lugin.command.whitelist");
        this.description = "Gerencia a whitelist do servidor.";
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            ctx.sendMessage("§cVocê deve especificar um subcomando: on, off, players");
            return;
        }

        switch (subCommand) {
            case "on" -> {
                if (platform.getWhitelistService().isWhitelistEnabled()) {
                    ctx.sendMessage("§cA whitelist já está ativada.");
                    return;
                }
                platform.getWhitelistService().setWhitelistEnabled(true);
                ctx.sendMessage("§aWhitelist ativada.");
            }
            case "off" -> {
                if (!platform.getWhitelistService().isWhitelistEnabled()) {
                    ctx.sendMessage("§cA whitelist já está desativada.");
                    return;
                }
                platform.getWhitelistService().setWhitelistEnabled(false);
                ctx.sendMessage("§aWhitelist desativada.");
            }
            case "players" -> {
                final int players = ctx.getBigIntegerOrThrow(1, "§cVocê deve especificar o número máximo de jogadores na whitelist.").intValue();
                if (players <= 0) {
                    throw new CommandFailedException("§cO número máximo de jogadores deve ser maior que zero .");
                }

                platform.getWhitelistService().setWhitelistPlayers(players);
                ctx.sendMessage("§aNúmero máximo de jogadores na whitelist definido para §b" + players + "§a.");
            }
            default -> throw new CommandFailedException("§cSubcomando inválido. Use: on, off ou players.");
        }
    }
}
