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

public class TpHereCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public TpHereCommand(@NotNull EssentialsPlugin plugin) {
        super("tphere", "hollow.tphere");
        this.plugin = plugin;
        this.setAliases(List.of("s"));
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player sender = ctx.getSenderAsPlayer();
        String targetName = ctx.getRawArgOrThrow(0, msg().get("invalid-usage", "usage", "/tphere <player>"));

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            msg().send(sender, "player-not-found", "player", targetName);
            return;
        }

        plugin.getTeleportService().teleport(target, sender.getLocation());
        msg().send(sender, "tp-teleported-here", "player", target.getName());
    }

    @Override
    public List<String> performTabComplete(CommandContext ctx) {
        if (ctx.getArgs().length == 0) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        if (ctx.getArgs().length == 1) {
            String prefix = ctx.getArgs()[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix))
                .toList();
        }
        return List.of();
    }
}
