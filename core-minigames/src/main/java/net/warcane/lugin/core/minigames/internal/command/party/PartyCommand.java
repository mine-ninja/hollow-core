package net.warcane.lugin.core.minigames.internal.command.party;

import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minigames.MinigamesPlatform;
import net.warcane.lugin.core.minigames.internal.command.party.subcommands.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PartyCommand extends SimpleCommand {

    private final MinigamesPlatform platform;

    public PartyCommand(MinigamesPlatform minigamesPlatform) {
        super("party");
        this.platform = minigamesPlatform;
        subCommands = List.of(
            new PartyInviteSubcommand(platform),
            new PartyAcceptSubcommand(platform),
            new PartyDenySubcommand(platform),
            new PartyOpenSubcommand(minigamesPlatform),
            new PartyCloseSubcommand(minigamesPlatform),
            new PartyJoinSubcommand(minigamesPlatform),
            new PartyDeleteSubcommand(minigamesPlatform),
            new PartyKickSubcommand(minigamesPlatform),
            new PartyInfoSubcommand(minigamesPlatform),
            new PartyLeaveSubcommand(minigamesPlatform),
            new PartyTransferSubcommand(minigamesPlatform)
        );
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();
        player.sendMessage("§e/party convidar §f- Envia um convite de party;");
        player.sendMessage("§e/party aceitar <jogador> §f- Aceita um convite de party;");
        player.sendMessage("§e/party negar <jogador> §f- Rejeita um convite de party;");
        player.sendMessage("§e/party abrir §f- Torna a party pública;");
        player.sendMessage("§e/party fechar §f- Torna a party privada;");
        player.sendMessage("§e/party entrar <jogador> §f- Entra em uma party pública;");
        player.sendMessage("§e/party deletar §f- Deleta a party;");
        player.sendMessage("§e/party expulsar <jogador> §f- Remove um jogador da party;");
        player.sendMessage("§e/party info §f- Veja as informações da sua party;");
        player.sendMessage("§e/party sair §f- Sair da party;");
        player.sendMessage("§e/party transferir <jogador> §f- Transfere a party para outro membro;");
        player.sendMessage("§e/p (mensagem) §f- Envia uma mensagem no chat da party;");
    }
}
