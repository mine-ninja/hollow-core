package net.warcane.lugin.core.minecraft.teleport;

import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.connection.ConnectionStatus;
import net.warcane.lugin.core.location.RemoteServerLocation;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class TeleportTrafficListener implements Listener {

    private final BukkitPlatform platform;
    private final TeleportManager teleportManager;

    @Contract(pure = true)
    public TeleportTrafficListener(@NotNull BukkitPlatform platform) {
        this.platform = platform;
        this.teleportManager = platform.getTeleportManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerWaitingToTeleport(@NonNull PlayerJoinEvent event) {
        var player = event.getPlayer();
        var userId = player.getUniqueId();

        if (teleportManager.hasPendingRequest(userId)) {
            Tasks.runSyncLater(() -> processPendingRequest(player), 1L);
        }
    }

    private void processPendingRequest(@NotNull Player player) {
        var packet = teleportManager.getPendingRequest(player.getUniqueId());
        if (packet == null || packet.getStatus() != ConnectionStatus.ALLOWED) return;

        if (packet.getLocation() != null) {
            teleportToLocation(player, packet.getLocation(), packet.getFallbackMessage());
        } else if (packet.getTargetId() != null) {
            var targetPlayer = Bukkit.getPlayer(packet.getTargetId());

            if (targetPlayer != null && targetPlayer.isOnline())
                teleportToPlayer(player, targetPlayer, packet.getFallbackMessage());
            else {
                player.kick(Component.text("§cEste jogador não está online ou nunca entrou em nosso servidor."));
            }
        } else if (!Objects.equals(packet.getTargetServerId(), platform.getGameServer().serverId())) {
            player.kick(Component.text("§cSolicitação de teletransporte inválida."));
        }
    }

    private void teleportToLocation(@NonNull Player player, @NonNull RemoteServerLocation position, String fallbackMessage) {
        World world = Bukkit.getWorld(position.worldName());
        if (world == null) {
            player.kick(Component.text("§cO mundo em que você quer se teleportar não foi encontrado."));
            return;
        }

        Location bukkitLocation = new Location(
            world,
            position.x(),
            position.y(),
            position.z(),
            position.yaw(),
            position.pitch()
        );

        player.teleport(bukkitLocation);
        if (fallbackMessage != null && !fallbackMessage.isEmpty()) {
            StringUtils.send(player, "<green>" + fallbackMessage);
        }
    }

    private void teleportToPlayer(@NonNull Player player, @NonNull Player targetPlayer, String fallbackMessage) {
        RemoteServerLocation position = new RemoteServerLocation(
            platform.getGameServer().serverId(),
            targetPlayer.getLocation().getWorld().getName(),
            targetPlayer.getLocation().getX(),
            targetPlayer.getLocation().getY(),
            targetPlayer.getLocation().getZ(),
            targetPlayer.getLocation().getYaw(),
            targetPlayer.getLocation().getPitch());

        teleportToLocation(player, position, fallbackMessage);
    }
}
