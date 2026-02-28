package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.essentials.model.Home;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HomeCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public HomeCommand(@NotNull EssentialsPlugin plugin) {
        super("home", "hollow.home");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        String name = ctx.getRawArgOrNull(0);
        if (name == null) name = "home";

        final String homeName = name;

        Thread.startVirtualThread(() -> {
            Home home = plugin.getHomeService().getHome(player.getUniqueId(), homeName);
            if (home == null) {
                msg().send(player, "home-not-found", "name", homeName);
                return;
            }

            Location loc = plugin.getHomeService().toLocation(home);
            if (loc == null) {
                msg().send(player, "home-not-found", "name", homeName);
                return;
            }

            plugin.getTeleportService().teleport(player, loc);
            msg().send(player, "home-teleported", "name", homeName);
        });
    }

    @Override
    public @NotNull List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (ctx.getArgs().length == 1) {
            // Suggest home names — loaded from cache only (non-blocking)
            List<String> names = plugin.getHomeService().getHomeNames(ctx.getSenderAsPlayer().getUniqueId());
            return filterStartingWith(names, ctx.getArgs()[0]);
        }
        return NONE_ARGS;
    }
}

