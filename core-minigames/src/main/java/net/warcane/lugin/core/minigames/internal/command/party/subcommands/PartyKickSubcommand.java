package net.warcane.lugin.core.minigames.internal.command.party.subcommands;

import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.command.subcommand.SimpleSubCommand;
import net.warcane.lugin.core.minigames.MinigamesPlatform;

public class PartyKickSubcommand extends SimpleSubCommand {

    private final MinigamesPlatform platform;

    public PartyKickSubcommand(MinigamesPlatform platform) {
        super("expulsar");
        this.platform = platform;
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();
        var targetPlayerName = ctx.getRawArgOrThrow(0, "§cVocê deve informar o nome de um jogador para expulsar da party.");
        platform.getPartyService().kickParty(targetPlayerName, player);
    }
}
