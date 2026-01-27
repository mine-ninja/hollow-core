package io.github.minehollow.minecraft.tell;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.PlayerUpdateAccountCachePacket;
import io.github.minehollow.sdk.player.preference.PreferenceRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ToggleTellCommand extends SimpleCommand {

    @NotNull
    private final BukkitPlatform platform;

    public ToggleTellCommand(@NotNull BukkitPlatform platform) {
        super("toggletell");
        this.platform = platform;
        setAliases(List.of("tt"));
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();

        platform.getPlayerAccountService()
            .getPlayerAccount(player.getUniqueId())
            .thenComposeAsync(account -> {
                if (account == null) throw new CommandFailedException("§cConta de jogador não encontrada.");

                var state = account.getPreference(PreferenceRegistry.PRIVATE_MESSAGES_ID);

                account.setPreference(PreferenceRegistry.PRIVATE_MESSAGES_ID, !state);
                player.sendMessage("§aSua preferência de mensagens privadas foi alterada com sucesso. \nMensagens privadas estão agora " + (state ? "§cdesativadas§a." : "§aativadas§a."));
                return platform.getPlayerAccountService().updatePlayerAccount(account).thenAcceptAsync(playerAccount -> {
                    platform.getNetworkClient().sendNetworkPacket(NetworkChannel.OPERATION, new PlayerUpdateAccountCachePacket(account.uniqueId()));
                });
            }, platform.getExecutorService());
    }
}
