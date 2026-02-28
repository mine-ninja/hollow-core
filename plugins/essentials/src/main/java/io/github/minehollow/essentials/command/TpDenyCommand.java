package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpDenyCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public TpDenyCommand(@NotNull EssentialsPlugin plugin) {
        super("tpdeny", "hollow.tpa");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();

        var requesterId = plugin.getTpaService().denyRequest(player.getUniqueId());
        if (requesterId == null) {
            msg().send(player, "tpa-no-pending");
            return;
        }

        msg().send(player, "tpa-denied");

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester != null && requester.isOnline()) {
            msg().send(requester, "tpa-denied-sender", "player", player.getName());
        }
    }
}

