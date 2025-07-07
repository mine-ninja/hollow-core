package net.warcane.lugin.core.minecraft.internal.command.lobby;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


public class LobbyCommand extends SimpleCommand {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private final BukkitPlatform platform;
    private final List<UUID> queuedPlayers = new ArrayList<>();

    public LobbyCommand(BukkitPlatform platform) {
        super("lobby");
        this.setAliases(List.of("hub"));
        this.platform = platform;
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        final var player = ctx.getSenderAsPlayer();

        if (queuedPlayers.contains(player.getUniqueId())) {
            queuedPlayers.remove(player.getUniqueId());
            player.sendMessage("§cSua ida ao lobby foi cancelada.");
        } else {
            queuedPlayers.add(player.getUniqueId());
            player.sendMessage("§aVocê irá para o lobby em 3 segundos. Use o comando /lobby novamente para cancelar.");
            player.playSound(player.getLocation(), "lugin:command.success", 1.0f, 1.0f);
            sendToLobby(player);
        }

    }

    private void sendToLobby(Player player) {
        Tasks.runAsyncLater(() -> {
            if (!queuedPlayers.contains(player.getUniqueId())) return;

            // todo implementar lógica de seleção de servidor de lobby
            final var lobbyServer = platform.getGameServerService()
              .queryServersByCategoryType(ServerCategoryType.LOBBY)
              .stream()
              .findFirst()
              .orElse(null);

            queuedPlayers.remove(player.getUniqueId());

            if(lobbyServer != null){

            }

        }, 20 * 3);
    }
}
