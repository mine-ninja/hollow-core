package io.github.minehollow.minecraft.tell;

import io.github.minehollow.sdk.database.RedisConnector;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.sdk.player.state.PlayerNetworkStateManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReplyCommand extends SimpleCommand implements ITell {

    @NotNull
    private final BukkitPlatform platform;

    public ReplyCommand(@NotNull BukkitPlatform platform) {
        super("r");
        setAliases(List.of("w", "msg", "whisper"));
        this.platform = platform;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();

        if (ctx.isArgsLength(0)) {
            StringUtils.send(player, "<red>Você deve informar a mensagem a ser respondida.");
            return;
        }

        CompletableFuture.runAsync(() -> RedisConnector.getInstance().useJedis(jedis -> {
            if (!jedis.exists(LAST_TELL_KEY + player.getUniqueId())) {
                StringUtils.send(player, "<red>Você não tem ninguém para responder.");
                return;
            }

            var lastSender = UUID.fromString(jedis.get(LAST_TELL_KEY + player.getUniqueId()));
            var lastSenderState = PlayerNetworkStateManager.getInstance().getPlayerState(lastSender);

            sendMessage(player, lastSenderState, ctx, true, platform);
        }));
    }
}
