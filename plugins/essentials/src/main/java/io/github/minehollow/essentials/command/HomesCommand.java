package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HomesCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public HomesCommand(@NotNull EssentialsPlugin plugin) {
        super("homes", "hollow.home");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();

        Thread.startVirtualThread(() -> {
            List<String> names = plugin.getHomeService().getHomeNames(player.getUniqueId());
            if (names.isEmpty()) {
                msg().send(player, "home-list-empty");
            } else {
                String joined = String.join(", ", names);
                msg().send(player, "home-list", "homes", joined);
            }
        });
    }
}

