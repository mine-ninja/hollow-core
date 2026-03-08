package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DelHomeCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public DelHomeCommand(@NotNull EssentialsPlugin plugin) {
        super("delhome", "hollow.delhome");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        String name = ctx.getRawArgOrThrow(0, msg().get("invalid-usage", "usage", "/delhome <name>"));

        Thread.startVirtualThread(() -> {
            boolean removed = plugin.getHomeService().deleteHome(player.getUniqueId(), name);
            if (removed) {
                msg().send(player, "home-deleted", "name", name);
            } else {
                msg().send(player, "home-not-found", "name", name);
            }
        });
    }

    @Override
    public @NotNull List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.getArgs().length == 1) {
            List<String> names = plugin.getHomeService().getHomeNames(ctx.getSenderAsPlayer().getUniqueId());
            return filterStartingWith(names, ctx.getArgs()[0]);
        }
        return NONE_ARGS;
    }
}
