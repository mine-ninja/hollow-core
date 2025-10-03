package net.warcane.lugin.core.minecraft.internal.command.staff;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.staff.StaffMessagePacket;
import net.warcane.lugin.core.player.subscription.SubscriptionCategoryType;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class VanishCommand extends SimpleCommand {

    private static final String VANISH_MESSAGE = "§dVocê entrou no vanish com sucesso.";
    private static final String UNVANISH_MESSAGE = "§dVocê saiu do vanish com sucesso.";

    @SuppressWarnings("unused")
    private final BukkitPlatform platform;

    public VanishCommand(BukkitPlatform platform) {
        super("v", "lugin.vanish");
        this.setAliases(List.of("vanish"));
        this.platform = platform;
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();
        final var manager = BukkitPlatform.getInstance().getVanishManager();

        if (manager.isVanished(player)) {
            manager.unvanish(player);
            player.sendMessage(UNVANISH_MESSAGE);
        } else {
            manager.vanish(player);
            player.sendMessage(VANISH_MESSAGE);
        }
    }
}
