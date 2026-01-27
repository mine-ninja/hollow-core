package io.github.minehollow.minecraft.tell;

import io.github.minehollow.sdk.database.RedisConnector;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.BukkitPlatformPlugin;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.sdk.player.preference.PreferenceRegistry;
import io.github.minehollow.sdk.player.state.PlayerNetworkState;
import io.github.minehollow.sdk.player.subscription.SubscriptionCategoryType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.params.SetParams;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public interface ITell {

    String LAST_TELL_KEY = "last-tell:";

    default void sendMessage(Player player, @Nullable PlayerNetworkState targetPlayer, CommandContext ctx, boolean skipName, BukkitPlatform platform) {
        if (targetPlayer == null) {
            StringUtils.send(player, "<red>Usuário não está online.");
            return;
        }

        if (targetPlayer.playerId().equals(player.getUniqueId())) {
            StringUtils.send(player, "<red>Falar consigo mesmo costuma ser um sinal de loucura. Procure um especialista urgente!");
            return;
        }

        var senderId = player.getUniqueId();
        var targetId = targetPlayer.playerId();
        var argsMessage = String.join(" ", Arrays.copyOfRange(ctx.getArgs(), skipName ? 0 : 1, ctx.getArgs().length));

        var senderAccount = platform.getPlayerAccountService().getPlayerAccount(senderId);
        var targetAccount = platform.getPlayerAccountService().getPlayerAccount(targetId);

        CompletableFuture.allOf(senderAccount, targetAccount)
            .thenRunAsync(() -> {
                var senderAcc = senderAccount.join();
                var targetAcc = targetAccount.join();

                if (senderAcc == null) {
                    BukkitPlatformPlugin.getInstance().getLogger().severe("Erro ao obter a conta do remetente ao enviar mensagem privada.");
                    StringUtils.send(player, "<red>Ocorreu um erro ao processar sua solicitação. Tente novamente mais tarde.");
                    return;
                }

                if (targetAcc == null) {
                    StringUtils.send(player, "<red>O jogador não está online.");
                    return;
                }

                if (argsMessage.isEmpty()) {
                    StringUtils.send(player, "<red>Você deve informar uma mensagem para enviar.");
                    return;
                }

                if (!senderAcc.getPreference(PreferenceRegistry.PRIVATE_MESSAGES_ID)) {
                    StringUtils.send(player, "<red>Você desativou o envio de mensagens privadas. Use /toggle para ativá-lo.");
                    return;
                }

                if (!targetAcc.getPreference(PreferenceRegistry.PRIVATE_MESSAGES_ID) && !senderAcc.getHighestSubscription().group().isStaffGroup()) {
                    StringUtils.send(player, "<red>Este usuário está com o recebimento de mensagens privadas desativado.");
                    return;
                }

                if (!new TellEvent(player, targetId, argsMessage).callEvent()) {
                    return;
                }

                var toMsg = "§8Mensagem para " + targetAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§8: §6" + argsMessage;
                player.sendMessage(toMsg);

                var fromMsg = "§8Mensagem de " + senderAcc.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL) + "§8: §6" + argsMessage;
                var targetBukkitPlayer = Bukkit.getPlayer(targetId);

                if (targetBukkitPlayer != null) {
                    targetBukkitPlayer.sendMessage(fromMsg);
                } else {
                    BukkitPlatform.getInstance().sendMessageToPlayer(targetId, fromMsg);
                }

                RedisConnector.getInstance().useJedis(jedis ->
                    jedis.set(LAST_TELL_KEY + targetId, senderId.toString(), SetParams.setParams().ex(60 * 5))
                );
            });
    }
}
