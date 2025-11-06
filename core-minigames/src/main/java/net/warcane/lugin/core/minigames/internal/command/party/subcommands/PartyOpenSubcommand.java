package net.warcane.lugin.core.minigames.internal.command.party.subcommands;

import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.command.subcommand.SimpleSubCommand;
import net.warcane.lugin.core.minigames.MinigamesPlatform;

public class PartyOpenSubcommand extends SimpleSubCommand {

    private final MinigamesPlatform platform;

    public PartyOpenSubcommand(MinigamesPlatform platform) {
        super("abrir");
        this.platform = platform;
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();
        platform.getPartyService().openParty(player);
    }
}
