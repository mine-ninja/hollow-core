package io.github.minehollow.essentials.command;

import io.github.minehollow.essentials.EssentialsPlugin;
import io.github.minehollow.essentials.config.MessageConfig;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TpaCommand extends SimpleCommand {

    private final EssentialsPlugin plugin;

    public TpaCommand(@NotNull EssentialsPlugin plugin) {
        super("tpa", "hollow.tpa");
        this.plugin = plugin;
        this.playersOnly = true;
    }

    private MessageConfig msg() { return plugin.getMessageConfig(); }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Player sender = ctx.getSenderAsPlayer();
        String targetName = ctx.getRawArgOrThrow(0, msg().get("invalid-usage", "usage", "/tpa <player>"));

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            msg().send(sender, "player-not-found", "player", targetName);
            return;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            throw new CommandFailedException(msg().get("invalid-usage", "usage", "/tpa <player>"));
        }

        boolean created = plugin.getTpaService().createRequest(sender.getUniqueId(), target.getUniqueId());
        if (!created) {
            msg().send(sender, "tpa-already-pending", "target", target.getName());
            return;
        }

        msg().send(sender, "tpa-sent", "target", target.getName());
        msg().send(target, "tpa-received", "player", sender.getName());
    }
}

