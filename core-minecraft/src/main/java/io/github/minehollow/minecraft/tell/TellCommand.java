package io.github.minehollow.minecraft.tell;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.sdk.player.state.PlayerNetworkStateManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TellCommand extends SimpleCommand implements ITell {

    @NotNull
    private final BukkitPlatform platform;

    public TellCommand(@NotNull BukkitPlatform platform) {
        super("tell");
        this.platform = platform;
        setAliases(List.of("w", "msg", "whisper"));
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        var player = ctx.getSenderAsPlayer();
        if (ctx.isArgsLength(0) || ctx.isArgsLength(1)) {
            StringUtils.send(player, "<red>Você deve informar o nome do jogador e a mensagem que deseja enviar.");
            return;
        }

        var targetName = ctx.getRawArgOrNull(0);
        var targetPlayer = PlayerNetworkStateManager.getInstance().getFromName(targetName);

        Tasks.runAsync(() -> sendMessage(player, targetPlayer, ctx, false, platform));
    }
}
