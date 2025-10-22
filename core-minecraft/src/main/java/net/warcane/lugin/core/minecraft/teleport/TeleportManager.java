package net.warcane.lugin.core.minecraft.teleport;

import net.warcane.lugin.core.connection.ConnectionReason;
import net.warcane.lugin.core.location.RemoteServerLocation;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.connection.ConnectionHandshakePacket;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.server.GameServer;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.server.type.ServerSubCategoryType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class TeleportManager {

    private final BukkitPlatform platform;
    private final GameServer currentServer;

    private final Map<UUID, ConnectionHandshakePacket> pendingRequests = new ConcurrentHashMap<>();

    public TeleportManager(@NonNull BukkitPlatform platform) {
        this.platform = platform;
        this.currentServer = platform.getGameServer();
    }

    public void addPendingRequest(UUID userId, ConnectionHandshakePacket packet) {
        pendingRequests.put(userId, packet);
        Tasks.runAsyncLater(() -> pendingRequests.remove(userId), 20 * 5L);
    }

    public void sendConnectionRequest(ConnectionHandshakePacket packet) {
        platform.getNetworkClient().sendNetworkPacket(
            NetworkChannel.PLAYER_CONNECTION,
            packet
        );
    }

    public void sendConnectionRequest(Player player, Supplier<ConnectionHandshakePacket> packetSupplier) {
        if (player != null && player.isOnline()) player.leaveVehicle();
        var packet = packetSupplier.get();

        if (packet != null) sendConnectionRequest(packet);
    }

    public ConnectionHandshakePacket getPendingRequest(UUID userId) {
        return pendingRequests.remove(userId);
    }

    public boolean hasPendingRequest(UUID userId) {
        return pendingRequests.containsKey(userId);
    }

    public boolean connectToGameServer(
        Player player, ServerCategoryType categoryType,
        ServerSubCategoryType subCategory, ConnectionReason reason,
        String fallbackMessage) {
        var gameServer = platform.getGameServerService().queryServersByCategoryType(categoryType)
            .stream()
            .filter(server -> server.subCategory() == subCategory)
            .min(Comparator.comparing(server -> server.serverPlayers().online()));
        if (gameServer.isEmpty()) return false;

        sendConnectionRequest(ConnectionHandshakePacket.toServer(
            player.getUniqueId(),
            BukkitPlatform.getInstance().getGameServer().serverId(),
            reason,
            gameServer.get().serverId(),
            fallbackMessage
        ));
        return true;
    }

    public void teleport(PlayerAccount profile, @NonNull UUID targetUserId, ConnectionReason reason, String fallbackMessage) {
        teleportInternal(
            profile,
            Bukkit.getPlayer(targetUserId),
            reason,
            targetUserId,
            fallbackMessage);
    }

    public void teleport(PlayerAccount profile, @NonNull String targetUsername, ConnectionReason reason, String fallbackMessage) {
        teleportInternal(
            profile,
            Bukkit.getPlayer(targetUsername),
            reason,
            targetUsername,
            fallbackMessage);
    }

    private void teleportInternal(
        @NonNull PlayerAccount profile,
        @Nullable Player targetPlayer,
        @NonNull ConnectionReason reason,
        @NonNull Object targetIdentifier,
        @NonNull String fallbackMessage) {
        var player = Bukkit.getPlayer(profile.uniqueId());
        if (player == null || !player.isOnline()) return;

        sendConnectionRequest(player, () -> {
            if (targetPlayer != null && targetPlayer.isOnline()) {
                player.teleport(targetPlayer.getLocation());
                StringUtils.send(player, "<green>" + fallbackMessage);
                return null;
            }

            if (targetIdentifier instanceof UUID uuid) {
                return ConnectionHandshakePacket.toPlayerById(
                    profile.uniqueId(), currentServer.serverId(),
                    reason, uuid, fallbackMessage
                );
            }


            return ConnectionHandshakePacket.toPlayerByName(
                profile.uniqueId(), currentServer.serverId(), reason,
                (String) targetIdentifier, fallbackMessage
            );
        });
    }

    public void teleport(Player player, RemoteServerLocation RemoteServerLocation, ConnectionReason reason) {
        teleport(player, RemoteServerLocation, reason, "Você foi teletransportado com sucesso.");
    }

    public void teleport(Player player, RemoteServerLocation RemoteServerLocation, ConnectionReason reason, String fallbackMessage) {
        teleport(player, RemoteServerLocation.targetServerId(), RemoteServerLocation, reason, fallbackMessage);
    }

    public void teleport(
        Player player, String targetServerId,
        RemoteServerLocation remoteServerLocation,
        ConnectionReason reason, String fallbackMessage
    ) {
        if (player == null || !player.isOnline()) return;

        sendConnectionRequest(player, () -> {
            if (Objects.equals(targetServerId, currentServer.serverId())) {
                handleLocalTeleport(player, remoteServerLocation, fallbackMessage);
                return null;
            }

            return ConnectionHandshakePacket.toPosition(
                player.getUniqueId(), currentServer.serverId(),
                reason, remoteServerLocation, fallbackMessage
            );
        });
    }

    private void handleLocalTeleport(Player player, @NonNull RemoteServerLocation RemoteServerLocation, String fallbackMessage) {
        var world = Bukkit.getWorld(RemoteServerLocation.worldName());
        if (world == null) {
            StringUtils.send(player, "<red>O mundo em que você quer se teleportar não foi encontrado.");
            return;
        }

        player.teleport(new Location(
            world,
            RemoteServerLocation.x(),
            RemoteServerLocation.y(),
            RemoteServerLocation.z(),
            RemoteServerLocation.yaw(),
            RemoteServerLocation.pitch()));
        StringUtils.send(player, "<green>" + fallbackMessage);
    }
}
