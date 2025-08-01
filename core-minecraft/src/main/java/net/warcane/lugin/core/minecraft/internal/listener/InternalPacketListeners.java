package net.warcane.lugin.core.minecraft.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.util.nametag.NameTags;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.SendMessageToPlayerPacket;
import net.warcane.lugin.core.network.packet.impl.player.SendModernMessageToPlayerPacket;
import net.warcane.lugin.core.network.packet.impl.player.SendSoundToPlayerPacket;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerReceiveGroupPacket;
import net.warcane.lugin.core.network.packet.impl.player.teleport.PlayerTeleportToLocationPacket;
import net.warcane.lugin.core.network.packet.impl.player.teleport.PlayerTeleportToTargetPacket;
import net.warcane.lugin.core.network.packet.impl.staff.StaffMessagePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.util.property.Property;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.warcane.lugin.core.minecraft.util.LocationUtil.convertToRemoteLocation;

@RequiredArgsConstructor
@Slf4j
public class InternalPacketListeners {

    private final BukkitPlatform platform;

    public void setup() {
        platform.getNetworkClient().registerPacketListener(SendMessageToPlayerPacket.class, new SendMessagePacketListener());
        platform.getNetworkClient().registerPacketListener(StaffMessagePacket.class, new StaffMessagePacketListener());
        platform.getNetworkClient().registerPacketListener(PlayerReceiveGroupPacket.class, new PlayerGroupReceivePacketListener(platform));
        platform.getNetworkClient().registerPacketListener(SendMessageToPlayerPacket.class, new MessageToPlayerPacketListener());
        platform.getNetworkClient().registerPacketListener(SendModernMessageToPlayerPacket.class, new ModernMessageToPlayerPacketListener());
        platform.getNetworkClient().registerPacketListener(SendSoundToPlayerPacket.class, new SendSoundToPlayerPacketListener());
        platform.getNetworkClient().registerPacketListener(PlayerTeleportToTargetPacket.class, new TargetedTeleportListener());
    }

    public static class TargetedTeleportListener implements PacketListener<PlayerTeleportToTargetPacket> {
        @Override
        public void onReceivePacket(@NotNull PlayerTeleportToTargetPacket packet, @NotNull Headers headers) {
            final var playerToTeleport = Bukkit.getPlayer(packet.playerId());
            final var targetPlayer = Bukkit.getPlayer(packet.targetId());
            if (playerToTeleport != null && targetPlayer != null) { // se ambos os jogadores existirem, teleporta o jogador localmente...
                playerToTeleport.teleportAsync(targetPlayer.getLocation());
            } else if (targetPlayer != null) { // se só o alvo existir, teleporta o jogador remotamente...

                final var destination = convertToRemoteLocation(targetPlayer.getLocation());
                final var teleportToLocationPacket = new PlayerTeleportToLocationPacket(packet.playerId(), destination);

                BukkitPlatform.getInstance()
                  .getNetworkClient()
                  .sendNetworkPacket(NetworkChannel.OPERATION, teleportToLocationPacket);
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
            if (player != null) {
                player.sendMessage(packet.message());
            } else {
                log.info("Player is null for received simple message packet {}", packet);
            }
        }
    }


    @RequiredArgsConstructor
    public static class PlayerGroupReceivePacketListener implements PacketListener<PlayerReceiveGroupPacket> {

        private final BukkitPlatform platform;

        @Override
        public void onReceivePacket(@NotNull PlayerReceiveGroupPacket packet, @NotNull Headers headers) {
            final var player = Bukkit.getPlayer(packet.playerId());
            if (player == null) return;

            final var category = packet.categoryType();
            final var expectedCategory = platform.getSubscriptionCategoryType();
            if (category != expectedCategory) return;


            final var groupPrefix = packet.receivedGroup().getPrefix();
            final var priority = packet.receivedGroup().getPriorityValue();
            final var groupColor = packet.receivedGroup().getNamedTextColor();

            final var loadTagsOnJoin = Property.get("LOAD_TAGS_ON_JOIN", "true").equalsIgnoreCase("true");
            if (loadTagsOnJoin) {
                NameTags.setNameTag(player, groupPrefix, "", priority, groupColor);
            }
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
            if (player != null) {
                player.sendMessage(packet.getMessage());
//
            }
        }
    }

    public static class SendSoundToPlayerPacketListener implements PacketListener<SendSoundToPlayerPacket> {
        @Override
        public void onReceivePacket(@NotNull SendSoundToPlayerPacket packet, @NotNull Headers headers) {
            final var player = Bukkit.getPlayer(packet.playerId());
            if (player != null) {
                player.playSound(player.getLocation(), packet.soundName(), packet.volume(), packet.pitch());
            }
        }
    }
}
