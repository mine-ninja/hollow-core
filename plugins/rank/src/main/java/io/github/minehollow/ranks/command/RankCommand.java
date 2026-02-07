package io.github.minehollow.ranks.command;

import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.ranks.RanksPlugin;
import io.github.minehollow.ranks.menu.RankLevelListMenu;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RankCommand extends SimpleCommand {

    private final RanksPlugin plugin;

    public RankCommand(@NotNull RanksPlugin plugin) {
        super("rank");
        this.plugin = plugin;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var sender = ctx.getSender();
        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cEste comando só pode ser executado por jogadores.");
                return;
            }

            MenuUtil.openMenu(player, RankLevelListMenu.class);
            return;
        }

        if (!sender.hasPermission("ranks.admin")) {
            sender.sendMessage("§cVocê não tem permissão para executar este comando.");
            return;
        }

        switch (subCommand.toLowerCase()) {
            case "addLevel" -> handleAddLevel(ctx);
            case "removeLevel" -> handleRemoveLevel(ctx);
            case "checkcost" -> handleCheckCost(ctx);
            default -> throw new CommandFailedException("Sub-comando inválido.");
        }
    }

    public void handleAddLevel(@NotNull CommandContext ctx) {
        final var target = ctx.getLocalPlayerOrThrow(1, "Use: /rank addLevel <player> <level>");
        final int level = ctx.getIntOrThrow(2, "Use: /rank addLevel <player> <level>");

        Thread.startVirtualThread(() -> {
            final var progress = plugin.getPlayerRankProgressService().getCachedProgress(target.getUniqueId());
            if (progress == null) {
                ctx.getSender().sendMessage("§cProgresso de rank do jogador não encontrado.");
                return;
            }

            progress.setCurrentRank(level);
            plugin.getPlayerRankProgressService().updatePlayerProgress(progress);
        });
    }

    public void handleRemoveLevel(@NotNull CommandContext ctx) {

    }

    public void handleCheckCost(@NotNull CommandContext ctx) {
        final int level = ctx.getIntOrThrow(1, "Use: /rank checkcost <level>");
        final var cost = plugin.getMoneyCostForRank(level);
        if (cost < 0) {
            ctx.getSender().sendMessage("§cCusto para o nível " + level + " não encontrado.");
        } else {
            ctx.getSender().sendMessage("§aCusto para o nível " + level + ": §e" + cost);
        }
    }
}
