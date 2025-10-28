package net.warcane.lugin.core.minigames.internal.command.party;

import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minigames.MinigamesPlatform;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PartyChatCommand extends SimpleCommand {
    private final MinigamesPlatform platform;

    public PartyChatCommand(MinigamesPlatform platform) {
        super("p");
        setAliases(List.of("partychat", "partyc", "pc"));
        this.platform = platform;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();

        if (ctx.getArgs().length == 0) {
            player.sendMessage("§cUso correto: /p <mensagem>");
            return;
        }

        var message = String.join(" ", ctx.getArgs());
        platform.getPartyService().sendPartyChatMessage(player, message);
    }
}
