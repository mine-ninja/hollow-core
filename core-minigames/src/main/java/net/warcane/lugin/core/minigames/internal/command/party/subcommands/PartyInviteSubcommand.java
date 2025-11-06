package net.warcane.lugin.core.minigames.internal.command.party.subcommands;

import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.event.ClickEvent;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.command.subcommand.SimpleSubCommand;
import net.warcane.lugin.core.minecraft.util.message.ComponentBuilder;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.minigames.MinigamesPlatform;
import net.warcane.lugin.core.minigames.MinigamesPlatformPlugin;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.party.PartyInvitePacket;
import net.warcane.lugin.core.player.state.PlayerNetworkStateManager;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;

@Slf4j
public class PartyInviteSubcommand extends SimpleSubCommand {

    private final MinigamesPlatform platform;

    public PartyInviteSubcommand(MinigamesPlatform platform) {
        super("convidar");
        this.platform = platform;
    }

    @Override
    protected void performSubCommand(CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();
        var targetName = ctx.getRawArgOrThrow(0, "§cVocê deve informar o nome de um jogador para convidar.");
        if (player.getName().equalsIgnoreCase(targetName)) {
            StringUtils.send(player, "<red>Você não pode convidar você mesmo para uma party.");
            return;
        }

        if (platform.getPartyService().isPlayerInParty(targetName)) {
            StringUtils.send(player, "<red>Esse jogador já está em uma party.");
            return;
        }

        if (platform.getPartyService().partyInviteExists(player.getName(), targetName)) {
            StringUtils.send(player, "<red>O jogador já possui um convite de party pendente.\nAguarde ele responder antes de enviar outro convite.");
            return;
        }

        var targetPlayer = PlayerNetworkStateManager.getInstance().getFromName(targetName);

        if (targetPlayer == null) {
            StringUtils.send(player, "<red>Usuário não está online.");
            return;
        }

        var senderAccount = platform.getPlayerAccountService().getPlayerAccount(player.getUniqueId());
        var targetAccount = platform.getPlayerAccountService().getPlayerAccount(targetPlayer.playerId());

        CompletableFuture.allOf(senderAccount, targetAccount)
            .thenRunAsync(() -> {
                var senderAcc = senderAccount.join();
                var targetAcc = targetAccount.join();

                if (senderAcc == null) {
                    StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                    return;
                }

                if (targetAcc == null) {
                    StringUtils.send(player, "<red>O jogador não está online.");
                    return;
                }

                new ComponentBuilder()
                    .newLine()
                    .simple("§eVocê enviou um pedido de party para " + targetAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§e.")
                    .newLine()
                    .simple("§eEle tem 60 segundos para aceitar ou recusar.")
                    .newLine()
                    .send(MinigamesPlatformPlugin.getInstance().adventure().player(player));

                var targetBukkitPlayer = Bukkit.getPlayer(targetAcc.uniqueId());
                var inviteMessage = new ComponentBuilder()
                    .newLine()
                    .simple(senderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + " §econvidou você para a party!")
                    .newLine()
                    .simple("§eClique ")
                    .actionHover("§a§lAQUI §epara aceitar o convite ou ", ClickEvent.runCommand("/party aceitar " + player.getName()), "<l-gray>Clique para aceitar o convite da party.")
                    .actionHover("§c§lAQUI", ClickEvent.runCommand("/party negar " + player.getName()), "<l-gray>Clique para recusar o convite da party.")
                    .simple(" §epara recusar.")
                    .newLine();
                if (targetBukkitPlayer != null) {
                    inviteMessage.send(MinigamesPlatformPlugin.getInstance().adventure().player(targetBukkitPlayer));
                } else {
                    platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, PartyInvitePacket.create(targetAcc.uniqueId(), inviteMessage.build()));
                }

                platform.getPartyService().createPartyInvite(senderAcc.playerName(), targetAcc.playerName());
            }, platform.getExecutorService());
    }
}
