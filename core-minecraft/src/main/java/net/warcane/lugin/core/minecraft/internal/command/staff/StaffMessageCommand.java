package net.warcane.lugin.core.minecraft.internal.command.staff;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.staff.StaffMessagePacket;
import org.jetbrains.annotations.NotNull;


public class StaffMessageCommand extends SimpleCommand {

    private static final String FORMAT = "§d§l[§dStaff§d§l] {player}§f: {msg}";

    private final BukkitPlatform platform;

    public StaffMessageCommand(BukkitPlatform platform) {
        super("s");
        this.platform = platform;
        this.requiredPermission = "lugin.staff";
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var serverId = platform.getId();
        final var player = ctx.getSenderAsPlayer();
        final var account = platform.getPlayerAccountService().getCachedAccount(player.getUniqueId());
        if (account == null) {
            throw new CommandFailedException("§cOcorreu um erro ao executar o comando. Tente novamente mais tarde.");
        }


        final var tagFormat = account.getFormattedDisplayName();
        final var msg = ctx.isArgsLength(0) ? "" : ctx.joinArgs(0);
        if (msg.isEmpty()) {
            throw new CommandFailedException("§cVocê precisa informar uma mensagem para enviar.");
        }

        final var format = FORMAT.replace("{player}", tagFormat)
          .replace("{msg}", msg);

        final var staffMsgPacket = new StaffMessagePacket(player.getUniqueId(), format);
        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_MESSAGE, staffMsgPacket);
    }
}
