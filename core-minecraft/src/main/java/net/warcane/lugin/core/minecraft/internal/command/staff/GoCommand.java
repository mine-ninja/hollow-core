package net.warcane.lugin.core.minecraft.internal.command.staff;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.vanish.VanishManager;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.staff.GoCommandPacket;
import net.warcane.lugin.core.network.packet.impl.staff.StaffMessagePacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class GoCommand extends SimpleCommand {

    private static final String COMMAND_SUCCESS = "§aEnviando você à %s...";
    private static final String NAME_NOT_INSERTED = "§cVocê precisa inserir o nome do jogador.";

    @SuppressWarnings("unused")
    private final BukkitPlatform platform;

    public GoCommand(BukkitPlatform platform) {
        super("btp", "lugin.staff");

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

        Player target = Bukkit.getPlayer(name);

        if (target != null) {
            if (player.hasPermission("lugin.vanish"))
                BukkitPlatform.getInstance().getVanishManager().vanish(player);

            player.sendMessage(COMMAND_SUCCESS.formatted(target.getName()));
            player.teleport(target);
            return;
        }

        final var goMsgPacket = new GoCommandPacket(player.getUniqueId(), name);
        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.GO, goMsgPacket);
    }
}
