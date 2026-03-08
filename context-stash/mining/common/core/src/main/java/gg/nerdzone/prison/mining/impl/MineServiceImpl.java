/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.impl;

import gg.nerdzone.common.RedisService;
import gg.nerdzone.prison.mining.api.events.MinePreResetEvent;
import gg.nerdzone.prison.mining.api.events.MineResetEvent;
import gg.nerdzone.prison.mining.area.factory.MineAreaFactory;
import gg.nerdzone.prison.mining.cache.MineCache;
import gg.nerdzone.prison.mining.enums.MineResetReason;
import gg.nerdzone.prison.mining.model.area.MineArea;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.model.user.MiningUser;
import gg.nerdzone.prison.mining.packet.MinePacket;
import gg.nerdzone.prison.mining.services.MineService;
import gg.nerdzone.prison.mining.services.MiningPacketService;
import gg.nerdzone.prison.mining.services.MiningUserService;
import gg.nerdzone.prison.mining.util.MinePlayerUtil;
import gg.nerdzone.prison.mining.util.MineServer;
import gg.nerdzone.prison.model.PrisonUserProfile;
import gg.nerdzone.prison.scheduler.CoreSchedulerHandler;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.lucko.helper.Events;
import me.lucko.helper.Services;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MineServiceImpl implements MineService {

    public static @NotNull MineServiceImpl create(@NonNull RedisService redisService) {
        return new MineServiceImpl(MineCache.create(redisService));
    }

    private final MineCache cache;

    @Override
    public @NotNull Optional<Mine> findMineById(@NotNull UUID mineId) {
        final Optional<Mine> mine = Optional.ofNullable(this.cache.find(mineId));
        mine.ifPresent(this::initArea);
        return mine;
    }

    @Override
    public @NotNull Mine findOrCreate(@NonNull PrisonUserProfile profile) {
        final Mine mine = this.cache.findOrCreate(profile);
        this.initArea(mine);
        return mine;
    }

    @Override
    public void reset(@NotNull Mine mine, @Nullable MiningUser user, @NotNull MineResetReason reason) {
        final long startedAt = System.currentTimeMillis();
        final MiningUserService userService = Services.load(MiningUserService.class);

        this.refreshMine(mine, userService); // Invalidate unnecessary data

        if (!mine.canReset() && reason == MineResetReason.MANUAL) {
            if (user != null) {
                // Reset cooldown message
            }
        }

        final MinePreResetEvent preResetEvent = Events.callAndReturn(new MinePreResetEvent(mine, !Bukkit.isPrimaryThread()));
        if (preResetEvent.isCancelled()) {
            // internal canceled
            return;
        }

        // Teleport all members to the spawn location (we need to teleport them in the main thread)
        CoreSchedulerHandler.INSTANCE.run(
            "mine-reset-teleport",
            () -> {
                mine.forEachMembers(
                    member -> mine.getArea().isInArea(member.getLocation()),
                    member -> {
                        if (member.hasMetadata("vanished")) {
                            return;
                        }

                        final Location location = member.getLocation();
                        location.setY(mine.getArea().getMaxY() + 2);
                        member.teleport(location);
                    }
                );
            }
        );

        mine.preReset(reason == MineResetReason.MANUAL);

        mine.reset(() -> {
            final ResetPacket resetPacket = this.createResetPacket(mine, mine.getArea(), true, userService);
            resetPacket.packets().forEach(packet -> resetPacket.packetService().sendMinePacket(mine, packet));

            mine.completeReset();
        }).thenAccept($ -> {
            Events.call(new MineResetEvent(mine, reason, startedAt, System.currentTimeMillis(), !Bukkit.isPrimaryThread()));

            if (user != null) {
                // success reset
            }
        }).exceptionally(throwable -> {
            if (user != null) {
                // reset failed
            }

            mine.completeReset();
            throwable.printStackTrace();
            return null;
        });

        // success
    }

    @Override
    public void unload(@NonNull Mine mine) {
        this.cache.invalidateLocal(mine);
    }

    public void refreshMine(@NotNull Mine mine, @NotNull MiningUserService userService) {
        mine.refresh();

        final Plugin plugin = Bukkit.getPluginManager().getPlugin("mining");
        final Runnable refreshAttributes = () -> mine.forEachMembers(
            null, (viewer) -> MinePlayerUtil.setMineAttributes(viewer, userService, Objects.requireNonNull(plugin))
        );

        if (!Bukkit.isPrimaryThread()) {
            CoreSchedulerHandler.INSTANCE.run("mines-refresh-attributes", refreshAttributes);
        } else {
            refreshAttributes.run();
        }
    }

    @Internal
    private ResetPacket createResetPacket(Mine mine, MineArea mineArea, boolean recreate, MiningUserService userService) {
        final MiningPacketService packetService = Services.load(MiningPacketService.class);
        final MiningUser owner = userService.search(mine.getOwnerId());

        return new ResetPacket(packetService, owner, packetService.createResetPackets(mine, mineArea, recreate));
    }

    @Internal
    private void initArea(@NotNull Mine mine) {
        if (MineServer.isMineServer()) {
            if (mine.getArea() == null) {
                final MineArea area = MineAreaFactory.INSTANCE.createArea(mine, -1);
                if (area == null) {
                    return;
                }

                mine.initArea(area);
                this.cache.insert(mine);
            }
        }
    }

    record ResetPacket(MiningPacketService packetService, MiningUser user, List<MinePacket<?>> packets) {
    }

}
