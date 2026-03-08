/*
 * Copyright (c) 2024-2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.area.factory;

import gg.nerdzone.prison.mining.config.RankConfig;
import gg.nerdzone.prison.mining.impl.MineSkinServiceImpl;
import gg.nerdzone.prison.mining.model.area.MineArea;
import gg.nerdzone.prison.mining.model.block.MineBlockPosition;
import gg.nerdzone.prison.mining.model.setting.MineSettings;
import gg.nerdzone.prison.mining.model.theme.MineTheme;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.model.user.MiningUser;
import gg.nerdzone.prison.mining.services.MiningUserService;
import gg.nerdzone.prison.model.PrisonUserProfile;
import gg.nerdzone.prison.service.PrisonUserProfileService;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import me.lucko.helper.Services;
import org.bukkit.Material;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MineAreaFactory {

    public static final MineAreaFactory INSTANCE = new MineAreaFactory();

    private final MiningUserService userService;
    private final PrisonUserProfileService profileService;
    private final MineSkinServiceImpl skinService;

    private MineAreaFactory() {
        this.userService = Services.load(MiningUserService.class);
        this.profileService = Services.load(PrisonUserProfileService.class);
        this.skinService = Services.load(MineSkinServiceImpl.class);
    }

    @Internal
    public @Nullable MineArea createArea(@NotNull Mine mine, int level) {
        final MiningUser user = this.userService.search(mine.getOwnerId());
        if (user == null) {
            return null;
        }

        // Skin logic (Skin = world, Theme = Raw (skins, name, etc))
        final MineTheme theme = Objects.requireNonNull(MineTheme.findByName(mine.getThemeId()));
        final MineBlockPosition center = this.skinService.getMiningAreaCenter(theme);
        final Supplier<Integer> levelSupplier = this.createLevelSupplier(user.getName());

        final int currentLevel = level != -1 ? level : levelSupplier.get();
        final int defaultSize = (MineSettings.DEFAULT_SIZE / 2) + RankConfig.getRankExpansion(currentLevel);
        final MineBlockPosition min = center.offset(-defaultSize, -(MineSettings.DEFAULT_DEPTH), -defaultSize);
        final MineBlockPosition max = center.offset(defaultSize, 0, defaultSize);
        return new MineArea(min, max, levelSupplier, this.createBlocksSupplier(user.getName()));
    }

    @Internal
    private Supplier<Integer> createLevelSupplier(@NotNull String username) {
        return () -> {
            final PrisonUserProfile user = this.profileService.getProfile(username);
            if (user == null) {
                return 0;
            }

            return user.getLevel();
        };
    }

    @Internal
    private Supplier<Set<Material>> createBlocksSupplier(@NotNull String username) {
        return () -> Set.of(Material.STONE);
    }

}
