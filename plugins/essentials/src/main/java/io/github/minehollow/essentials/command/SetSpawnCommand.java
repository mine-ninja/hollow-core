package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SetSpawnCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public SetSpawnCommand(@NotNull EssentialsPlugin plugin) {
        super("setspawn", "hollow.setspawn");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        plugin.getSpawnService().setSpawn(player.getLocation());
        msg().send(player, "spawn-set");
    }
}

