package net.warcane.lugin.core.minecraft;

import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.plugin.SimplePlugin;
import net.warcane.lugin.core.permission.PlayerGroup;
import net.warcane.lugin.core.server.GameServer;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BukkitPlatformPlugin extends SimplePlugin {

    private BukkitPlatform bukkitPlatform;

    @Override
    public void onEnable() {
        bukkitPlatform = BukkitPlatform.provide(this, ServerCategoryType.LOBBY);
        bukkitPlatform.init();

        registerCommands("lugin", new ListServersCommand());
    }

    final class ListServersCommand extends SimpleCommand {
        public ListServersCommand() {
            super("listservers");
            this.requiredGroup = PlayerGroup.MANAGER;
        }

        @Override
        public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
            List<GameServer> gameServers = bukkitPlatform.getGameServerService().queryAllServersInNetwork();
            if (gameServers.isEmpty()) {
                ctx.getSender().sendMessage("§cNenhum servidor encontrado.");
                return;
            }

            final int size = gameServers.size();
            final var message = new StringBuilder("§aServidores disponíveis §7(").append(size).append("):\n");
            for (GameServer server : gameServers) {
                final var address = server.hostAddress().toString();
                final var playerCount = server.playerCount().toFormattedString();
                final var serverCategory = server.categoryType().name();

                message.append("§7- §a").append(server.serverId())
                  .append(" §7(§b").append(playerCount).append("§7) §8[§e").append(serverCategory).append("§8] §7- ")
                  .append(address).append("\n");
            }

            ctx.sendMessage(message.toString());
        }
    }
}
