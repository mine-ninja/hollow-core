package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public SpawnCommand(@NotNull EssentialsPlugin plugin) {
        super("spawn", "hollow.spawn");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();

        if (!plugin.getSpawnService().isSet()) {
            msg().send(player, "spawn-not-set");
            return;
        }

        plugin.getTeleportService().teleport(player, plugin.getSpawnService().getSpawn());
        msg().send(player, "spawn-teleported");
    }
}

