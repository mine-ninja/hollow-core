package net.warcane.lugin.core.minigames.internal.command.party.subcommands;

import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.command.subcommand.SimpleSubCommand;
import net.warcane.lugin.core.minigames.MinigamesPlatform;

public class PartyAcceptSubcommand extends SimpleSubCommand {

    private final MinigamesPlatform platform;

    public PartyAcceptSubcommand(MinigamesPlatform platform) {
        super("aceitar");
        this.platform = platform;
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();
        var senderName = ctx.getRawArgOrThrow(0, "§cVocê deve informar o nome do jogador que enviou o convite.");

        platform.getPartyService().acceptPartyInvite(senderName, player);
    }
}
