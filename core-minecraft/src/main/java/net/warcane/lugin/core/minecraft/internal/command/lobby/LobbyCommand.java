package net.warcane.lugin.core.minecraft.internal.command.lobby;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class LobbyCommand extends SimpleCommand implements Listener {

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
        final var playerId = player.getUniqueId();

        if (queuedPlayers.contains(playerId)) {
            queuedPlayers.remove(playerId);
            player.sendMessage("§cSua ida ao lobby foi cancelada.");
            player.playSound(player.getLocation(), "lugin:command.cancel", 1.0f, 1.0f);
        } else {
            queuedPlayers.add(playerId);
            player.sendMessage("§aVocê irá para o lobby em 3 segundos. Use o comando /lobby novamente para cancelar.");
            player.playSound(player.getLocation(), "lugin:command.success", 1.0f, 1.0f);
            sendToLobby(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void cleanupOnPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        queuedPlayers.remove(event.getPlayer().getUniqueId());
    }

    private void sendToLobby(Player player) {
        final var playerId = player.getUniqueId();
        Tasks.runAsyncLater(() -> {
            if (queuedPlayers.contains(playerId)) {
                queuedPlayers.remove(playerId);
                platform.tryConnectPlayerToServerCategory(player.getUniqueId(), ServerCategoryType.LOBBY);
            }
        }, 20 * 3);
    }
}