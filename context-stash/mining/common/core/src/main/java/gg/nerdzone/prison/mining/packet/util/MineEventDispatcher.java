/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.packet.util;

import gg.nerdzone.prison.mining.api.events.MineEvent;
import gg.nerdzone.prison.mining.api.events.block.MineBlockBreakEvent;
import gg.nerdzone.prison.mining.api.events.block.MineMultiBlockBreakEvent;
import gg.nerdzone.prison.mining.api.events.player.MinePlayerEvent;
import gg.nerdzone.prison.mining.context.model.MineBlockBreakContextImpl;
import gg.nerdzone.prison.mining.enums.MineBreakReason;
import gg.nerdzone.prison.mining.model.block.MinePaletteBlock;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.model.user.MiningUser;
import gg.nerdzone.prison.mining.packet.MinePacket;
import gg.nerdzone.prison.mining.services.MiningUserService;
import gg.nerdzone.prison.mining.util.MineMaterialUtil;
import gg.nerdzone.prison.model.PrisonUserProfile;
import gg.nerdzone.prison.service.PrisonUserProfileService;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import me.lucko.helper.Events;
import me.lucko.helper.Services;
import org.apache.commons.lang3.Validate;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.annotation.concurrent.ThreadSafe;

/**
 * Responsible for dispatching mine events. All events are handled here.
 *
 * @see MineEvent
 * @see MinePacket
 * @see gg.nerdzone.prison.mining.services.MiningPacketService#sendMinePacket(Mine, MinePacket)  MiningPacketService - Send packets logic
 * @since 1.0.0
 */
@UtilityClass
public class MineEventDispatcher {

    private final Logger LOGGER = Logger.getLogger(MineEventDispatcher.class.getSimpleName());

    private MiningUserService userService;

    private PrisonUserProfileService profileService;

    /**
     * Dispatches a {@link MineBlockBreakEvent} or {@link MineMultiBlockBreakEvent} for the given mine and player.
     *
     * @param mine   The mine in which the blocks are being broken.
     * @param player The player breaking the blocks. Can be null if the blocks are broken by a natural cause.
     * @param blocks The blocks that are being broken.
     * @return The event that was dispatched.
     */
    public @NotNull MinePlayerEvent dispatchBlockBreak(@NonNull Mine mine, @Nullable Player player, MinePaletteBlock... blocks) {
        Validate.isTrue(blocks.length > 0, "positions cannot be empty.");

        final MineBreakReason reason = (player != null) ? MineBreakReason.PLAYER : MineBreakReason.NATURAL;
        final int totalBlocks = blocks.length;
        if (totalBlocks > 1) {
            return Events.callAndReturn(new MineMultiBlockBreakEvent(player, mine, reason, blocks));
        }

        final MiningUser user = getUser(player);
        final PrisonUserProfile profile = getProfile(player);
        final MinePaletteBlock block = blocks[0];
        final Material oldMaterial = MineMaterialUtil.toMaterial(mine.getArea().getBlock(block.x(), block.y(), block.z()));

        // Context is used here to check parameters and call event.
        return MineBlockBreakContextImpl.create(block.position(), oldMaterial, mine, user, profile == null ? 1 : profile.getLevel()).callBreakEvent();
    }

    @ApiStatus.Internal
    private MiningUser getUser(@Nullable Player player) {
        if (player == null) {
            return null;
        }

        final MiningUser user = getUserService().search(player.getName());
        if (user == null) {
            LOGGER.log(Level.WARNING, "MineUser is null for player: %s".formatted(player.getName()), new IllegalStateException());
        }

        return user;
    }

    @ApiStatus.Internal
    private @Nullable PrisonUserProfile getProfile(@Nullable Player player) {
        if (player == null) {
            return null;
        }

        final PrisonUserProfile profile = getProfileService().getProfile(player.getName());
        if (profile == null) {
            LOGGER.log(Level.WARNING, "PrisonUserProfile is null for player: %s".formatted(player.getName()), new IllegalStateException());
        }

        return profile;
    }

    @ApiStatus.Internal
    @ThreadSafe
    private @NotNull MiningUserService getUserService() {
        if (userService != null) {
            return userService;
        }

        userService = Services.load(MiningUserService.class);
        return userService;
    }

    @ApiStatus.Internal
    @ThreadSafe
    private @NotNull PrisonUserProfileService getProfileService() {
        if (profileService != null) {
            return profileService;
        }

        profileService = Services.load(PrisonUserProfileService.class);
        return profileService;
    }
}
