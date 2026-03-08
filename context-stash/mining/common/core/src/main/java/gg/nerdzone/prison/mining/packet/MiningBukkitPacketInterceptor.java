/*
 * Copyright (c) 2024-2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.User;
import gg.nerdzone.prison.mining.impl.MineSkinServiceImpl;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.model.user.MiningUser;
import gg.nerdzone.prison.mining.packet.impl.MineBukkitPacket;
import gg.nerdzone.prison.mining.services.MineService;
import gg.nerdzone.prison.mining.services.MiningUserService;
import lombok.NonNull;
import me.lucko.helper.Services;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * Intercepts incoming packets and converts them to {@link MinePacket}s.
 *
 * @see MiningPacketInterceptor PacketInterceptor interface for more information.
 */
public class MiningBukkitPacketInterceptor implements MiningPacketInterceptor<PacketSendEvent, PacketReceiveEvent, MineBukkitPacket<?>> {

    @Contract(pure = true)
    public static void init() {
        if (Services.get(MiningBukkitPacketInterceptor.class).isPresent()) {
            throw new IllegalStateException("MiningBukkitPacketInterceptor is already initialized.");
        }

        Services.provide(MiningBukkitPacketInterceptor.class, new MiningBukkitPacketInterceptor());
    }

    private final MineBukkitPacketFactory factory = MineBukkitPacketFactory.create();

    private final MineSkinServiceImpl skinService = Services.load(MineSkinServiceImpl.class);

    private final MineService mineService = Services.load(MineService.class);

    private final MiningUserService userService = Services.load(MiningUserService.class);

    @Override
    public void interceptIn(@NonNull PacketSendEvent event, String... viewers) {
        final MineMetadata data = this.validateData(event.getUser());
        if (data == null) {
            return;
        }

        this.factory.onSend(event, data);
    }

    @Override
    public void interceptOut(@NonNull PacketReceiveEvent event, String... viewers) {
        final MineMetadata data = this.validateData(event.getUser());
        if (data == null) {
            return;
        }

        this.factory.onReceive(event, data);
    }

    @Internal
    private @Nullable MiningBukkitPacketInterceptor.MineMetadata validateData(User eventUser) {
        final Player player = Bukkit.getPlayerExact(eventUser.getName());
        if (player == null || !(this.skinService.isSkinWorld(player.getWorld()))) {
            return null;
        }

        final MiningUser user = this.userService.search(eventUser.getName());
        if (user == null || user.getCurrentMineId() == null) {
            return null;
        }

        final Mine mine = this.mineService.findMine(user.getCurrentMineId());
        if (mine == null || mine.getArea() == null) {
            return null;
        }

        return new MineMetadata(user, mine);
    }

    public record MineMetadata(
        @NonNull MiningUser user,
        @NonNull Mine mine
    ) {
    }
}
