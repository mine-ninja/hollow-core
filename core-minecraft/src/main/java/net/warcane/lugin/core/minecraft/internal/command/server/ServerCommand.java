package net.warcane.lugin.core.minecraft.internal.command.server;

import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

public class ServerCommand extends SimpleCommand {
    public ServerCommand() {
        super("server");
        this.requiredPermission = "lugin.master";
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();
        player.sendMessage("§cComando em manutenção.");
    }
}
