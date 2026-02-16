package io.github.minehollow.minecraft.internal.command.staff;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.command.SimpleCommand;
import io.github.minehollow.minecraft.command.context.CommandContext;
import io.github.minehollow.minecraft.command.exception.CommandFailedException;
import org.jetbrains.annotations.NotNull;


public class StaffMessageCommand extends SimpleCommand {

    private static final String FORMAT = "§d§l[§dStaff§d§l] {player}§f: {msg}";

    private final BukkitPlatform platform;

    public StaffMessageCommand(BukkitPlatform platform) {
        super("s", "hollow.staff");
        this.platform = platform;
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();

//        final var tagFormat = account.getFormattedDisplayName(SubscriptionCategoryType.GLOBAL);
//        final var msg = ctx.isArgsLength(0) ? "" : ctx.joinArgs(0);
//        if (msg.isEmpty()) {
//            throw new CommandFailedException("§cVocê precisa informar uma mensagem para enviar.");
//        }
//
//        final var format = FORMAT.replace("{player}", tagFormat)
//          .replace("{msg}", msg);
//
//        final var staffMsgPacket = new StaffMessagePacket(player.getUniqueId(), format);
//        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, staffMsgPacket);
    }
}
