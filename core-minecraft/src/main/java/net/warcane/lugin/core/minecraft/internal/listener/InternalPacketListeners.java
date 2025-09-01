package net.warcane.lugin.core.minecraft.internal.listener;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.event.account.PlayerAccountLoadEvent;
import net.warcane.lugin.core.minecraft.internal.events.PlayerReceiveMessageEvent;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.vanish.VanishManager;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.SendMessageToPlayerPacket;
import net.warcane.lugin.core.network.packet.impl.player.SendModernMessageToPlayerPacket;
import net.warcane.lugin.core.network.packet.impl.player.SendSoundToPlayerPacket;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerLoseGroupPacket;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerReceiveGroupPacket;
import net.warcane.lugin.core.network.packet.impl.player.teleport.PlayerTeleportToLocationPacket;
import net.warcane.lugin.core.network.packet.impl.player.teleport.PlayerTeleportToTargetPacket;
import net.warcane.lugin.core.network.packet.impl.staff.GoCachePacket;
import net.warcane.lugin.core.network.packet.impl.staff.StaffMessagePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.UUID;

import static net.warcane.lugin.core.minecraft.task.Tasks.runSync;
import static net.warcane.lugin.core.minecraft.util.LocationUtil.convertToRemoteLocation;

@RequiredArgsConstructor
@Slf4j
public class InternalPacketListeners {

    private final BukkitPlatform platform;

    public void setup() {
        final var networkClient = platform.getNetworkClient();

        networkClient.registerPacketListener(SendMessageToPlayerPacket.class, new SendMessagePacketListener());
        networkClient.registerPacketListener(StaffMessagePacket.class, new StaffMessagePacketListener());
        networkClient.registerPacketListener(PlayerReceiveGroupPacket.class, new PlayerGroupReceivePacketListener(platform));
        networkClient.registerPacketListener(PlayerLoseGroupPacket.class, new PlayerLoseGroupPacketListener(platform));
        networkClient.registerPacketListener(SendModernMessageToPlayerPacket.class, new ModernMessageToPlayerPacketListener());
        networkClient.registerPacketListener(SendSoundToPlayerPacket.class, new SendSoundToPlayerPacketListener());
        networkClient.registerPacketListener(PlayerTeleportToTargetPacket.class, new TargetedTeleportListener());
        networkClient.registerPacketListener(GoCachePacket.class, new GoCacheListener());

        final var listener = new GoCacheListener();

        networkClient.registerPacketListener(GoCachePacket.class, listener);
        Bukkit.getPluginManager().registerEvents(listener, platform.getPlugin());
    }

    private static void updatePlayerPerms(BukkitPlatform platform, UUID uuid) {
        final var account = platform.getPlayerAccountService().getCachedAccount(uuid);
        if (account == null) return;

        final var player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        
        platform.getPermissionInjector().injectPermissions(player);
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

        @EventHandler(priority = EventPriority.LOWEST)
        public void onJoin(PlayerAccountLoadEvent event) {
            UUID uuid = event.getLoadedAccount().uniqueId();
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                UUID uuidTarget = goCache.get(uuid);

                if (uuidTarget != null) {
                    goCache.remove(uuid);

                    Tasks.runSync(() -> {
                        VanishManager vanishManager = BukkitPlatform.getInstance().getVanishManager();

                        vanishManager.vanish(player);

                        Player target = Bukkit.getPlayer(uuidTarget);

                        if (target != null) {
                            player.teleport(target);
                        }
                    });
                }
            }
        }

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
                if (onlinePlayer.hasPermission("lugin.staff")) {
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
                log.info("Player is null for received simple message packet {}", packet);
                return;
            }
            if (!new PlayerReceiveMessageEvent(player, null, packet.message(), packet.key()).callEvent()) {
                return;
            }
            player.sendMessage(packet.message());
        }
    }


    @RequiredArgsConstructor
    public static class PlayerGroupReceivePacketListener implements PacketListener<PlayerReceiveGroupPacket> {

        private final BukkitPlatform platform;

        @Override
        public void onReceivePacket(@NotNull PlayerReceiveGroupPacket packet, @NotNull Headers headers) {
            updatePlayerPerms(platform, packet.playerId());
        }
    }


    @RequiredArgsConstructor
    public static class PlayerLoseGroupPacketListener implements PacketListener<PlayerLoseGroupPacket> {

        private final BukkitPlatform platform;

        @Override
        public void onReceivePacket(@NotNull PlayerLoseGroupPacket packet, @NotNull Headers headers) {
            updatePlayerPerms(platform, packet.playerId());
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
}
