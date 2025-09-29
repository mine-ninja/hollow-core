package net.warcane.lugin.core.minecraft.internal.command.lobby;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.event.tick.AsyncServerTickEvent;
import net.warcane.lugin.core.minecraft.util.stopwatch.Stopwatch;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LobbyCommand extends SimpleCommand implements Listener {
    private final BukkitPlatform platform;

    private final Map<UUID, Long> queuedPlayers = new ConcurrentHashMap<>();
    private final Stopwatch timer = new Stopwatch();

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

        if (queuedPlayers.containsKey(playerId)) {
            queuedPlayers.remove(playerId);
            player.sendMessage("§aTeleportando para o lobby...");
            player.playSound(player.getLocation(), "lugin:command.success", 1.0f, 1.0f);
            platform.tryConnectPlayerToServerCategory(player.getUniqueId(), ServerCategoryType.LOBBY);
        } else {
            queuedPlayers.put(playerId, System.currentTimeMillis() + 3000);
            player.sendMessage("§aUse o comando /lobby novamente para teleportar para o lobby.");
            player.playSound(player.getLocation(), "lugin:command.success", 1.0f, 1.0f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void cleanupOnPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        queuedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCommandExpire(@NotNull AsyncServerTickEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            final var playerId = player.getUniqueId();
            final var expireTime = queuedPlayers.get(playerId);
            if (expireTime == null) continue;

            if (System.currentTimeMillis() >= expireTime) {
                queuedPlayers.remove(playerId);
                player.sendMessage("§cO tempo para usar o comando /lobby expirou. Use o comando novamente para teleportar.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            }
        }
    }
}
