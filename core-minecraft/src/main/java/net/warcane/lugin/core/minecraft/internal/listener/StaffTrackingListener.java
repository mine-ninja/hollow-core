package net.warcane.lugin.core.minecraft.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.event.account.PlayerAccountLoadEvent;
import net.warcane.lugin.core.minecraft.internal.command.staff.data.StaffOnlineData;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.network.packet.impl.player.permission.PlayerReceiveGroupPacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.data.RedisCache;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

@Slf4j
@RequiredArgsConstructor
public class StaffTrackingListener implements Listener, PacketListener<PlayerReceiveGroupPacket> {

    private final RedisCache<StaffOnlineData> staffOnlineCache = new RedisCache<>(StaffOnlineData.class);
    private static final String STAFF_ONLINE_KEY = "staff_online";

    @Override
    public void onReceivePacket(@NotNull PlayerReceiveGroupPacket packet, @NotNull Headers headers) {
        Tasks.runAsync(() -> {
            var playerId = packet.playerId();
            var player = Bukkit.getPlayer(packet.playerId());

            if (player == null) {
                return;
            }

            if (staffOnlineCache.hget(STAFF_ONLINE_KEY, playerId.toString()) != null && !packet.receivedGroup().isStaffGroup()) {
                staffOnlineCache.hdel(STAFF_ONLINE_KEY, playerId.toString());
                return;
            }

            var getGameServer = BukkitPlatform.getInstance().getGameServer();
            var staffOnlineData = new StaffOnlineData(player.getName(), getGameServer.serverId(), packet.receivedGroup());
            staffOnlineCache.hset(STAFF_ONLINE_KEY, playerId.toString(), staffOnlineData);
        });
    }

    @EventHandler
    public void onPlayerConnectedToServer(PlayerAccountLoadEvent event) {
        var bukkitPlatform = BukkitPlatform.getInstance();

        var gameServer = bukkitPlatform.getGameServer();
        if (gameServer == null || gameServer.categoryType() == ServerCategoryType.LOGIN) {
            return;
        }

        var playerAccount = event.getLoadedAccount();

        var player = Bukkit.getPlayer(playerAccount.uniqueId());

        if (player == null) {
            return;
        }

        var highestSubscription = playerAccount.getHighestSubscription(bukkitPlatform.getSubscriptionCategoryType());
        var group = highestSubscription.group();

        if (!group.isStaffGroup()) {
            return;
        }

        Tasks.runAsync(() -> {
            var staffOnlineData = new StaffOnlineData(player.getName(), gameServer.serverId(), group);
            staffOnlineCache.hset(STAFF_ONLINE_KEY, player.getUniqueId().toString(), staffOnlineData);
        });
    }

    @EventHandler
    public void onPlayerDisconnectedFromServer(PlayerQuitEvent event) {
        Tasks.runAsync(() -> staffOnlineCache.hdel(STAFF_ONLINE_KEY, event.getPlayer().getUniqueId().toString()));
    }
}
