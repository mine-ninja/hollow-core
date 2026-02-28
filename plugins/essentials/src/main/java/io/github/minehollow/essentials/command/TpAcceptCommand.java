package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TpAcceptCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public TpAcceptCommand(@NotNull EssentialsPlugin plugin) {
        super("tpaccept", "hollow.tpa");
        this.plugin = plugin;
        this.playersOnly = true;
        this.setAliases(List.of("tpyes"));
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player player = ctx.getSenderAsPlayer();

        var requesterId = plugin.getTpaService().acceptRequest(player.getUniqueId());
        if (requesterId == null) {
            msg().send(player, "tpa-no-pending");
            return;
        }

        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null || !requester.isOnline()) {
            msg().send(player, "player-not-found", "player", "???");
            return;
        }

        msg().send(player, "tpa-accepted");
        msg().send(requester, "tpa-accepted-sender", "player", player.getName());

        // Teleport the requester to this player's location
        plugin.getTeleportService().teleport(requester, player.getLocation());
    }
}

