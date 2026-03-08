package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EssentialsCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public EssentialsCommand(@NotNull EssentialsPlugin plugin) {
        super("essentials", "hollow.reload");
        this.plugin = plugin;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        String sub = ctx.getRawArgOrNull(0);
        if (sub == null || !sub.equalsIgnoreCase("reload")) {
            throw new CommandFailedException(msg().get("invalid-usage", "usage", "/hollowcore reload"));
        }

        if (ctx.getSender() instanceof Player player && !player.hasPermission("hollow.reload")) {
            msg().send(player, "no-permission");
            return;
        }

        plugin.reloadAll();

        if (ctx.getSender() instanceof Player player) {
            msg().send(player, "reload-success");
        } else {
            ctx.getSender().sendMessage("Configuration reloaded successfully.");
        }
    }
}

