package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TpCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public TpCommand(@NotNull EssentialsPlugin plugin) {
        super("tp", "hollow.tp");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player sender = ctx.getSenderAsPlayer();

        if (ctx.getArgs().length == 0) {
            throw new CommandFailedException(msg().get("invalid-usage", "usage", "/tp <player> [target]"));
        }

        if (ctx.getArgs().length == 1) {
            // /tp <player> — teleport self to player
            String targetName = ctx.getArgs()[0];
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                msg().send(sender, "player-not-found", "player", targetName);
                return;
            }

            plugin.getTeleportService().teleport(sender, target.getLocation());
            msg().send(sender, "tp-teleported", "target", target.getName());
        } else {
            // /tp <player> <target> — teleport player to target (admin)
            if (!sender.hasPermission("hollow.tp.others")) {
                msg().send(sender, "no-permission");
                return;
            }

            String playerName = ctx.getArgs()[0];
            String targetName = ctx.getArgs()[1];

            Player player = Bukkit.getPlayer(playerName);
            Player target = Bukkit.getPlayer(targetName);

            if (player == null) {
                msg().send(sender, "player-not-found", "player", playerName);
                return;
            }
            if (target == null) {
                msg().send(sender, "player-not-found", "player", targetName);
                return;
            }

            plugin.getTeleportService().teleport(player, target.getLocation());
            msg().send(sender, "tp-teleported", "target", target.getName());
        }
    }

    @Override
    public @NotNull List<String> performTabComplete(@NotNull CommandContext ctx) {
        return SimpleCommand.NONE_ARGS; // default player names
    }
}

