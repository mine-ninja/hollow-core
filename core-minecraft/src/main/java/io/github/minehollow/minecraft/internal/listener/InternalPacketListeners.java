package io.github.minehollow.minecraft.internal.listener;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.gamerule.listener.GameRuleUpdateListener;
import io.github.minehollow.minecraft.internal.events.PlayerReceiveMessageEvent;
import io.github.minehollow.sdk.network.channel.NetworkChannel;
import io.github.minehollow.sdk.network.packet.impl.gamerule.GameRuleUpdatePacket;
import io.github.minehollow.sdk.network.packet.impl.player.SendMessageToPlayerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.SendModernMessageToPlayerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.SendSoundToPlayerPacket;
import io.github.minehollow.sdk.network.packet.impl.player.discord.PlayerLinkedDiscordPacket;
import io.github.minehollow.sdk.network.packet.impl.player.discord.PlayerUnlinkedDiscordPacket;
import io.github.minehollow.sdk.network.packet.impl.player.teleport.PlayerTeleportToLocationPacket;
import io.github.minehollow.sdk.network.packet.impl.player.teleport.PlayerTeleportToTargetPacket;
import io.github.minehollow.sdk.network.packet.impl.staff.GoCachePacket;
import io.github.minehollow.sdk.network.packet.impl.staff.StaffMessagePacket;
import io.github.minehollow.sdk.network.packet.impl.wallet.WalletRefreshRequestPacket;
import io.github.minehollow.sdk.network.packet.listener.PacketListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

import static io.github.minehollow.minecraft.task.Tasks.runSync;
import static io.github.minehollow.minecraft.util.LocationUtil.convertToRemoteLocation;

@RequiredArgsConstructor
@Slf4j
public class InternalPacketListeners {

    private final BukkitPlatform platform;

    public void setup() {
        final var networkClient = platform.getNetworkClient();

        networkClient.registerPacketListener(SendMessageToPlayerPacket.class, new SendMessagePacketListener());
        networkClient.registerPacketListener(StaffMessagePacket.class, new StaffMessagePacketListener());
        networkClient.registerPacketListener(SendModernMessageToPlayerPacket.class, new ModernMessageToPlayerPacketListener());
        networkClient.registerPacketListener(SendSoundToPlayerPacket.class, new SendSoundToPlayerPacketListener());
        networkClient.registerPacketListener(PlayerTeleportToTargetPacket.class, new TargetedTeleportListener());
        networkClient.registerPacketListener(GoCachePacket.class, new GoCacheListener());
        networkClient.registerPacketListener(WalletRefreshRequestPacket.class, new WalletUpdateListener());
        networkClient.registerPacketListener(GameRuleUpdatePacket.class, new GameRuleUpdateListener(platform));
        networkClient.registerPacketListener(PlayerLinkedDiscordPacket.class, new PlayerLinkedDiscordPacketListener());
        networkClient.registerPacketListener(PlayerUnlinkedDiscordPacket.class, new PlayerUnlinkedDiscordPacketListener());

        final var listener = new GoCacheListener();

        networkClient.registerPacketListener(GoCachePacket.class, listener);
        Bukkit.getPluginManager().registerEvents(listener, platform.getPlugin());
    }


    public static class WalletUpdateListener implements PacketListener<WalletRefreshRequestPacket> {

        @Override
        public void onReceivePacket(@NotNull WalletRefreshRequestPacket packet, @NotNull Headers headers) {

        }
    }

    public static class TargetedTeleportListener implements PacketListener<PlayerTeleportToTargetPacket> {
        @Override
        public void onReceivePacket(@NotNull PlayerTeleportToTargetPacket packet, @NotNull Headers headers) {
            final var playerToTeleport = Bukkit.getPlayer(packet.playerId());
            final var targetPlayer = Bukkit.getPlayer(packet.targetId());
            if (playerToTeleport != null && targetPlayer != null) { // se ambos os jogadores existirem, teleporta o jogador localmente...
                runSync(() -> playerToTeleport.teleport(targetPlayer.getLocation()));
            } else if (targetPlayer != null) { // se só o alvo existir, teleporta o jogador remotamente...
                final var destination = convertToRemoteLocation(targetPlayer.getLocation());
                final var teleportToLocationPacket = new PlayerTeleportToLocationPacket(packet.playerId(), destination);

                BukkitPlatform.getInstance()
                  .getNetworkClient()
                  .sendNetworkPacket(NetworkChannel.OPERATION, teleportToLocationPacket);
            }
        }
    }

    public static class GoCacheListener implements PacketListener<GoCachePacket>, Listener {
        private static final HashMap<UUID, UUID> goCache = new HashMap<>();

        @Override
        public void onReceivePacket(@NotNull GoCachePacket packet, @NotNull Headers headers) {
            UUID playerUUID = packet.uniqueId();
            UUID targetUUID = packet.targetId();

            Player player = Bukkit.getPlayer(playerUUID);

            if (player != null) {
                Player target = Bukkit.getPlayer(targetUUID);

                if (target != null) {
                    player.teleport(target);
                }
            } else {
                goCache.put(playerUUID, targetUUID);
            }
        }
    }

    public static class StaffMessagePacketListener implements PacketListener<StaffMessagePacket> {
        @Override
        public void onReceivePacket(@NotNull StaffMessagePacket packet, @NotNull Headers headers) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("hollow.staff")) {
                    onlinePlayer.sendMessage(packet.message());
                }
            }
        }
    }

    public static class SendMessagePacketListener implements PacketListener<SendMessageToPlayerPacket> {
        @Override
        public void onReceivePacket(@NotNull SendMessageToPlayerPacket packet, @NotNull Headers headers) {

            Player player = Bukkit.getPlayer(packet.playerId());

            if (player == null) {
                log.debug("Player is null for received simple message packet {}", packet);
                return;
            }
            if (!new PlayerReceiveMessageEvent(player, null, packet.message(), packet.key()).callEvent()) {
                return;
            }
            player.sendMessage(packet.message());
        }
    }


    public static class MessageToPlayerPacketListener implements PacketListener<SendMessageToPlayerPacket> {
        @Override
        public void onReceivePacket(@NotNull SendMessageToPlayerPacket packet, @NotNull Headers headers) {
            Player player = Bukkit.getPlayer(packet.playerId());
            if (player != null) {
                player.sendMessage(packet.message());
            }
        }
    }

    public static class ModernMessageToPlayerPacketListener implements PacketListener<SendModernMessageToPlayerPacket> {

        @Override
        public void onReceivePacket(@NotNull SendModernMessageToPlayerPacket packet, @NotNull Headers headers) {
            Player player = Bukkit.getPlayer(packet.playerId());
            if (player == null) return;
            if (!new PlayerReceiveMessageEvent(player, packet.getMessage(), null, packet.key()).callEvent()) {
                return;
            }
            player.sendMessage(packet.getMessage());
        }
    }

    public static class SendSoundToPlayerPacketListener implements PacketListener<SendSoundToPlayerPacket> {
        @Override
        public void onReceivePacket(@NotNull SendSoundToPlayerPacket packet, @NotNull Headers headers) {
            final var player = Bukkit.getPlayer(packet.playerId());
            if (player == null) return;
            player.playSound(player.getLocation(), packet.soundName(), packet.volume(), packet.pitch());

        }
    }

    @RequiredArgsConstructor
    public static class PlayerLinkedDiscordPacketListener implements PacketListener<PlayerLinkedDiscordPacket> {

        @Override
        public void onReceivePacket(@NotNull PlayerLinkedDiscordPacket packet, @NotNull Headers headers) {
            Player player = Bukkit.getPlayer(packet.playerId());

            if (player == null) {
                log.debug("Player is null for received linked discord packet packet {}", packet);
                return;
            }

            if (!packet.message().isBlank()) {
                player.sendMessage(packet.message());
            }
        }
    }

    @RequiredArgsConstructor
    public static class PlayerUnlinkedDiscordPacketListener implements PacketListener<PlayerUnlinkedDiscordPacket> {

        @Override
        public void onReceivePacket(@NotNull PlayerUnlinkedDiscordPacket packet, @NotNull Headers headers) {
            Player player = Bukkit.getPlayer(packet.playerId());

            if (player == null) {
                log.debug("Player is null for received unlinked discord packet packet {}", packet);
                return;
            }

            if (!packet.message().isBlank()) {
                player.sendMessage(packet.message());
            }
        }
    }
}
