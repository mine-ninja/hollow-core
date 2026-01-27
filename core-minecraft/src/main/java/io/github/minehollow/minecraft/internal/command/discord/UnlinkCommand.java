package io.github.minehollow.minecraft.internal.command.discord;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.player.discord.PlayerUnlinkDiscordPacket;
import org.jetbrains.annotations.NotNull;

public class UnlinkCommand extends SimpleCommand {

    @NotNull
    private final BukkitPlatform platform;

    public UnlinkCommand(@NotNull BukkitPlatform platform) {
        super("desvincular");
        this.platform = platform;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();
        var playerUniqueId = player.getUniqueId();

        platform.getPlayerDiscordService().getPlayerDiscord(playerUniqueId)
            .whenCompleteAsync((playerDiscord, throwable) -> {
                if (throwable != null) {
                    throw new CommandFailedException("§cErro ao recuperar sua conta do Discord. Tente novamente mais tarde.", throwable);
                }

                if (playerDiscord == null || !playerDiscord.isLinked()) {
                    throw new CommandFailedException("§cSua conta não está vinculada ao Discord.");
                }

                var discordPackt = new PlayerUnlinkDiscordPacket(playerUniqueId);
                platform.getNetworkClient().sendNetworkPacket(NetworkChannel.OPERATION, discordPackt);
            });
    }
}
