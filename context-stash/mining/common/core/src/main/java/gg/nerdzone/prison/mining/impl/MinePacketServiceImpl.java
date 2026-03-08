/*
 * Copyright (c) 2024-2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.impl;

import static me.lucko.helper.Services.load;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import gg.nerdzone.prison.mining.model.area.MineArea;
import gg.nerdzone.prison.mining.model.theme.MineTheme;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.packet.MinePacket;
import gg.nerdzone.prison.mining.packet.MinePacketResponse;
import gg.nerdzone.prison.mining.packet.factory.MinePacketFactory;
import gg.nerdzone.prison.mining.packet.impl.MineBukkitPacket;
import gg.nerdzone.prison.mining.packet.impl.MineChunkPacket;
import gg.nerdzone.prison.mining.packet.impl.MineUnknownPacket;
import gg.nerdzone.prison.mining.packet.registry.MinePacketRegistry;
import gg.nerdzone.prison.mining.services.MiningPacketService;
import gg.nerdzone.prison.mining.services.MiningThemeService;
import gg.nerdzone.prison.mining.services.MiningUserService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.lucko.helper.Services;
import net.minecraft.server.MinecraftServer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of the mining packet service.
 *
 * @see MiningPacketService PacketService interface for more information.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MinePacketServiceImpl implements MiningPacketService {

    public static @NotNull MinePacketServiceImpl create() {
        return new MinePacketServiceImpl(load(MiningThemeService.class), load(MiningUserService.class), load(MineSkinServiceImpl.class));
    }

    private final Logger logger = Logger.getLogger(MinePacketServiceImpl.class.getSimpleName());

    public static final Set<MinePacket<?>> EMPTY_PACKET = Collections.emptySet();

    private final MiningThemeService themeService;

    private final MiningUserService userService;

    private final MineSkinServiceImpl skinService;

    private PacketEventsAPI<?> api;

    public static MiningPacketService get() {
        return Services.load(MiningPacketService.class);
    }

    @Override
    public @NonNull Set<MinePacket<?>> fetchMinePacket(@NonNull Mine mine) {
        final Optional<MineTheme> themeOpt = this.themeService.findTheme(mine.getThemeId());
        if (themeOpt.isEmpty()) {
            return EMPTY_PACKET;
        }

        final MineTheme theme = themeOpt.get();
        return new LinkedHashSet<>(this.createThemePacket(theme, mine));
    }

    @Override
    public @NonNull LinkedList<MinePacket<?>> createThemePacket(@NonNull MineTheme theme, @NonNull Mine mine) {
        // TODO: Implement place location

        return new LinkedList<>();
    }

    @Override
    public boolean isMiningPacket(@NonNull Object object) {
        return object instanceof MinePacket<?>;
    }

    @Override
    public List<MinePacket<?>> createResetPackets(@NotNull Mine mine, @NotNull MineArea area, boolean recreateChunks) {
        final MineTheme theme = MineTheme.findByName(mine.getThemeId());
        if (theme == null) {
            return new ArrayList<>(0);
        }

        final World world = this.skinService.findWorld(mine.getTheme());
        if (world == null) {
            return new ArrayList<>(0);
        }

        return MinePacketFactory.createResetPackets(world, mine, recreateChunks);
    }

    @Override
    public MinePacketResponse sendMinePacket(@Nullable Player source, @NotNull Mine mine, @NotNull MinePacket<?> packet, boolean forceSource) {
        packet.preSend();

        mine.forEachMembers(
            !forceSource ? this.notSource(source) : null, player -> {
                if (!(this.skinService.isSkinWorld(player.getWorld()))) {
                    return;
                }

                final MinePacketResponse response = this.sendMinePacket(player, packet);
                if (response == MinePacketResponse.INTERNAL_ERROR) {
                    this.logger.info("Failed to send a packet in mine %s, check the logs for more details.".formatted(mine.getMineId()));
                }
            }
        );

        packet.postSend();
        return MinePacketResponse.SUCCESSFULLY;
    }

    @Override
    public @NotNull MinePacketResponse sendMinePacket(@NotNull Player target, @NotNull MinePacket<?> packet) {
        if (MinecraftServer.getServer().isShutdown()) {
            return MinePacketResponse.INTERNAL_ERROR;
        }

        try {
            final MineBukkitPacket<?> bukkitPacket = this.ensurePacket(packet);
            final User user = this.api().getPlayerManager().getUser(target);
            if (user == null || user.getChannel() == null || !(ChannelHelper.isOpen(user.getChannel()))) {
                return MinePacketResponse.NO_USER;
            }

            if (bukkitPacket instanceof MineChunkPacket chunkPacket) { // Adapt chunk packet for multi packets sending (light + chunk data)
                chunkPacket.adapt(user);
                for (final PacketWrapper<?> wrapper : chunkPacket.getWrappers()) {
                    user.sendPacketSilently(wrapper);
                }
            } else {
                user.sendPacketSilently(bukkitPacket.wrapper());
            }

            return MinePacketResponse.SUCCESSFULLY;
        } catch (Exception exception) {
            final String playerData = ("Name: %s, Location: %s").formatted(target.getName(), target.getLocation());
            final String packetData = ("Class: %s, Data: %s").formatted(
                packet.getClass().getSimpleName(),
                (packet instanceof MineBukkitPacket<?> bukkitPacket) ? bukkitPacket.getWrapper().toString() : "null"
            );

            this.logger.log(
                Level.WARNING,
                "Packet service can't send packet to player: %s, packet: %s".formatted(playerData, packetData)
            );

            exception.printStackTrace();

            return MinePacketResponse.INTERNAL_ERROR;
        }
    }

    @Override
    public @NotNull MinePacketResponse simulatePacket(@Nullable Player source, @NotNull PacketWrapper<?> wrapper, boolean forceSource) {
        if (source == null) {
            return MinePacketResponse.NO_SOURCE;
        }

        if (!(this.skinService.isSkinWorld(source.getWorld()))) {
            return MinePacketResponse.NOT_MINE_WORLD;
        }

        final MinePacket<?> registeredPacket = MinePacketRegistry.createPacket(source, wrapper);
        final MinePacket<?> packet = (registeredPacket != null) ? registeredPacket : new MineUnknownPacket(wrapper, source);

        final Mine currentMine = this.userService.getCurrentMine(source.getName());
        if (currentMine == null) {
            return MinePacketResponse.NO_MINE;
        }

        return this.sendMinePacket(source, currentMine, packet, forceSource);
    }

    @Internal
    private MineBukkitPacket<?> ensurePacket(@NotNull MinePacket<?> packet) {
        if (!(packet instanceof MineBukkitPacket<?> bukkitPacket)) {
            throw new IllegalArgumentException("Invalid packet implementation: " + packet.getClass().getName());
        }

        if (!(packet.getWrapper() instanceof PacketWrapper<?>)) {
            throw new IllegalArgumentException("Invalid packet wrapper implementation: " + packet.getWrapper().getClass().getName());
        }

        return bukkitPacket;
    }

    @Internal
    private PacketEventsAPI<?> api() {
        return this.api != null ? this.api : (this.api = PacketEvents.getAPI());
    }
}
