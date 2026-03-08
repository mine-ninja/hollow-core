package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BackCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public BackCommand(@NotNull EssentialsPlugin plugin) {
        super("back", "hollow.back");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() {
        return plugin.getMessageConfig();
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        Location backLocation = plugin.getBackService().getBackLocation(player.getUniqueId());
        if (backLocation == null) {
            msg().send(player, "back-not-available");
            return;
        }

        plugin.getBackService().ignoreNextTeleportCapture(player.getUniqueId());
        plugin.getTeleportService().teleport(player, backLocation);
        msg().send(player, "back-teleported");
    }

    @Override
    public @NotNull List<String> performTabComplete(@NotNull CommandContext ctx) {
        return NONE_ARGS;
    }
}

