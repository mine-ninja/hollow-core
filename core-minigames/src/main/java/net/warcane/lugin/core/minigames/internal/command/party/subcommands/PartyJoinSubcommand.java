package net.warcane.lugin.core.minigames.internal.command.party.subcommands;

import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.command.subcommand.SimpleSubCommand;
import net.warcane.lugin.core.minigames.MinigamesPlatform;

public class PartyJoinSubcommand extends SimpleSubCommand {

    private final MinigamesPlatform platform;

    public PartyJoinSubcommand(MinigamesPlatform platform) {
        super("entrar");
        this.platform = platform;
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();
        var leaderName = ctx.getRawArgOrThrow(0, "§cVocê deve especificar o nome do líder da party.");
        platform.getPartyService().joinInParty(player, leaderName);
    }
}
