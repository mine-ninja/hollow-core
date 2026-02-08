package io.github.minehollow.lobby.command;

import io.github.minehollow.lobby.LobbyPlugin;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.util.message.StringUtils;
import org.jetbrains.annotations.NotNull;

public class SpawnCommand extends SimpleCommand {

    private final LobbyPlugin plugin;

    public SpawnCommand(@NotNull LobbyPlugin plugin) {
        super("spawn", "lobby.admin");
        this.plugin = plugin;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();
        final var subCommand = ctx.getRawArgOrNull(0);
        if (subCommand == null) {
            player.teleport(plugin.getSpawnLocation());
            return;
        }

        if (subCommand.toLowerCase().equals("set")) {
            plugin.setSpawnLocationAndSave(player.getLocation());
            player.sendMessage(StringUtils.formatString("<green>Spawn setado para sua localização atual."));
        } else {
            throw new CommandFailedException("Subcomando desconhecido: " + subCommand);
        }
    }
}
