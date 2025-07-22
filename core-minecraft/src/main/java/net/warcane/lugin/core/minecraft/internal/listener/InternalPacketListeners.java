package net.warcane.lugin.core.minecraft.internal.listener;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.util.nametag.NameTags;
import net.warcane.lugin.core.network.packet.impl.player.SendMessageToPlayerPacket;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerReceiveGroupPacket;
import net.warcane.lugin.core.network.packet.impl.staff.StaffMessagePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.util.property.Property;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class InternalPacketListeners {

    private final BukkitPlatform platform;

    public void setup() {
        platform.getNetworkClient().registerPacketListener(SendMessageToPlayerPacket.class, new SendMessagePacketListener());
        platform.getNetworkClient().registerPacketListener(StaffMessagePacket.class, new StaffMessagePacketListener());
        platform.getNetworkClient().registerPacketListener(PlayerReceiveGroupPacket.class, new PlayerGroupReceivePacketListener(platform));
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

            final var loadTagsOnJoin = Property.get("LOAD_TAGS_ON_JOIN", "true").equalsIgnoreCase("true");
            if (loadTagsOnJoin) {
                NameTags.setNameTag(player, groupPrefix, "", priority);
                NameTags.updateAllTags();
            }
        }
    }
}
