package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EnderChestCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public EnderChestCommand(@NotNull EssentialsPlugin plugin) {
        super("enderchest", "hollow.enderchest");
        this.plugin = plugin;
        this.playersOnly = true;
        this.setAliases(List.of("ec"));
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();
        player.openInventory(player.getEnderChest());
        msg().send(player, "enderchest-opened");
    }
}

