package io.github.minehollow.minecraft.internal.command.staff;

import io.github.minehollow.sdk.connection.ConnectionReason;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class GoCommand extends SimpleCommand {

    private static final String COMMAND_SUCCESS = "§aEnviando você à %s...";
    private static final String NAME_NOT_INSERTED = "§cVocê precisa inserir o nome do jogador.";

    @SuppressWarnings("unused")
    private final BukkitPlatform platform;

    public GoCommand(BukkitPlatform platform) {
        super("btp", "hollow.staff");

        this.setAliases(List.of("go"));
        this.platform = platform;
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();
        String name = ctx.getRawArgOrNull(0);

        if (name == null)
            throw new CommandFailedException(NAME_NOT_INSERTED);

        if (player.hasPermission("hollow.vanish")) {
            BukkitPlatform.getInstance().getVanishManager().vanish(player);
        }

        var playerAccount = platform.getPlayerAccountService().getCachedAccount(player.getUniqueId());
        platform.getTeleportManager().teleport(playerAccount, name, ConnectionReason.COMMAND, COMMAND_SUCCESS.formatted(name));

        /*if (target != null) {
            if (player.hasPermission("hollow.vanish"))
                BukkitPlatform.getInstance().getVanishManager().vanish(player);

            player.sendMessage(COMMAND_SUCCESS.formatted(target.getName()));
            player.teleport(target);
            return;
        }



        final var goMsgPacket = new GoCommandPacket(player.getUniqueId(), name);
        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.GO, goMsgPacket);*/
    }
}
