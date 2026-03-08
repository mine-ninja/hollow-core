/*
 * Copyright (c) 2024-2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.impl;

import static gg.nerdzone.common.util.MiniMessageUtil.miniMessage;

import com.google.common.base.Preconditions;
import gg.nerdzone.common.item.impl.ItemStackBuilder;
import gg.nerdzone.common.player.GlobalPlayer;
import gg.nerdzone.common.player.GlobalPlayerService;
import gg.nerdzone.common.progressbar.ProgressBar;
import gg.nerdzone.common.server.controller.ServerController;
import gg.nerdzone.common.server.location.ServerLocation;
import gg.nerdzone.prison.mining.area.factory.MineAreaFactory;
import gg.nerdzone.prison.mining.cache.MiningUserCache;
import gg.nerdzone.prison.mining.config.RankConfig;
import gg.nerdzone.prison.mining.config.conf.MineLevelConf;
import gg.nerdzone.prison.mining.enums.MineResetReason;
import gg.nerdzone.prison.mining.eventbus.impl.MiningUserLoadEvent;
import gg.nerdzone.prison.mining.eventbus.impl.MiningUserReadyEvent;
import gg.nerdzone.prison.mining.eventbus.impl.MiningUserSearchEvent;
import gg.nerdzone.prison.mining.eventbus.impl.MiningUserUnloadEvent;
import gg.nerdzone.prison.mining.model.area.MineArea;
import gg.nerdzone.prison.mining.model.theme.MineTheme;
import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.model.user.MiningUser;
import gg.nerdzone.prison.mining.position.MiningPosition;
import gg.nerdzone.prison.mining.services.MineService;
import gg.nerdzone.prison.mining.services.MiningUserService;
import gg.nerdzone.prison.mining.util.MineServer;
import gg.nerdzone.prison.model.PrisonUserProfile;
import gg.nerdzone.prison.service.PrisonUserProfileService;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.UnaryOperator;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.lucko.helper.Events;
import me.lucko.helper.Services;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of the mining user service.
 *
 * @see MiningUserService UserService interface for more information.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class MineUserServiceImpl implements MiningUserService {

    private static final Component RANKUP_BAR_PREFIX = ItemStackBuilder.message().deserialize("<#61b076>ʀᴀɴᴋᴜᴘ <dark_gray>[</dark_gray>");

    private static final Component EVENT_5X = MiniMessage.miniMessage().deserialize(" <gradient:#8de67a:#507d46><bold>EVENTO 5X</gradient>");

    private final MiningUserCache userCache;
    private final PrisonUserProfileService profileService;
    private final MineService mineService;
    private final MineSkinServiceImpl skinService;

    private final ServerController serverController;

    private final Map<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();

    public static @NotNull MineUserServiceImpl create() {
        return new MineUserServiceImpl(
            Services.load(MiningUserCache.class),
            Services.load(PrisonUserProfileService.class),
            Services.load(MineService.class),
            Services.load(MineSkinServiceImpl.class),
            Services.load(ServerController.class)
        );
    }

    @Override
    public @Nullable MiningUser load(@NonNull String username) {
        final PrisonUserProfile profile = this.profileService.getProfile(username);
        if (profile == null) {
            return null;
        }

        final MiningUser user = Objects.requireNonNull(this.search(username));
        final MiningUserLoadEvent event = Events.callAndReturn(new MiningUserLoadEvent(user, profile));
        return event.isCancelled() ? null : event.getUser();
    }

    @Override
    public @Nullable MiningUser search(@NonNull String username) {
        final PrisonUserProfile profile = this.profileService.getProfile(username);
        if (profile == null) {
            return null;
        }

        final MiningUser user = this.userCache.findOrCreate(username, profile);
        Events.call(new MiningUserSearchEvent(user));
        return user;
    }

    @Override
    public void reset(@NonNull MiningUser user) {
        final Mine mine = this.getCurrentMine(user.getName());
        if (mine == null) {
            return;
        }

        this.mineService.reset(mine, user, MineResetReason.MANUAL);
    }

    @Override
    public void reset(@NonNull PrisonUserProfile profile, boolean recalculateArea) {
        final MiningUser user = this.userCache.find(profile.getUsername());
        if (user == null) {
            return;
        }

        final Mine mine = this.getCurrentMine(profile.getUsername());
        if (mine == null) {
            return;
        }

        if (recalculateArea) {
            final MineArea area = MineAreaFactory.INSTANCE.createArea(mine, profile.getLevel());
            Preconditions.checkNotNull(area, "Failed to create mine area for reset.");

            mine.initArea(area);
        }

        this.mineService.reset(mine, user, MineResetReason.AUTOMATIC);
    }

    @Override
    public void ready(@NonNull String player) {
        final MiningUser user = this.userCache.find(player);
        if (user == null) {
            return;
        }

        Events.call(new MiningUserReadyEvent(user));
    }

    @Override
    public void unload(@NotNull Player player) {
        final MiningUser user = this.userCache.find(player.getName());
        if (user == null) {
            return;
        }

        final MiningUserUnloadEvent event = Events.callAndReturn(new MiningUserUnloadEvent(user));
        this.userCache.insert(event.getUser());
    }

    @Override
    public void toggleAutomaticReset(@NonNull Player player) {
        player.closeInventory();

        if (!player.hasPermission(AUTOMATIC_RESET_PERMISSION)) {
            player.sendMessage(miniMessage("<red>A função de reset automático é exclusiva de jogadores com VIP Esmeralda."));
            return;
        }

        final MiningUser user = this.userCache.find(player.getName());
        if (user == null) {
            player.sendMessage(miniMessage("<red>Não foi possível encontrar seus dados de mineração. Tente relogar no servidor."));
            return;
        }

        user.setAutomaticReset(!user.isAutomaticReset());
        this.userCache.insert(user);

        player.sendMessage(miniMessage(user.isAutomaticReset()
                                       ? "<green>Reset automático ativado. Sua mina será resetada automaticamente a cada 3s."
                                       : "<red>Reset automático desativado. Você precisará resetar sua mineração manualmente daqui pra frente."));
    }

    @Override
    public boolean isAutomaticResetEnabled(@NonNull Player player) {
        final MiningUser user = this.userCache.find(player.getName());
        return user != null && user.isAutomaticReset() && player.hasPermission(AUTOMATIC_RESET_PERMISSION);
    }

    @Override
    public boolean rankup(@NonNull Player player, boolean force, @NotNull UnaryOperator<Integer> operator) {
        final ReentrantLock lock = this.playerLocks.computeIfAbsent(player.getUniqueId(), $ -> new ReentrantLock());

        lock.lock();
        try {
            final PrisonUserProfile profile = this.profileService.getProfile(player.getName());
            if (profile == null) {
                return false;
            }

            if (profile.getLevel() >= RankConfig.MAX_RANK) {
                return false;
            }

            final int currentLevel = profile.getLevel();
            final double xpNeeded = MineLevelConf.RANKUP_FORMULA.apply(currentLevel);
            if (!force && profile.getExperience() < xpNeeded) {
                return false;
            }

            final int nextLevel = Math.min(operator.apply(currentLevel), RankConfig.MAX_RANK);

            // Try to rankup
            final boolean rankup = this.profileService.rankup(player.getName(), nextLevel, 0.0d) != null;
            if (rankup) {
                this.displayRankupActionbar(player);
            }

            return rankup;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void displayRankupActionbar(@NonNull Player player) {
        final PrisonUserProfile profile = this.profileService.getProfile(player.getName());
        if (profile == null) {
            return;
        }

        final double expRequired = MineLevelConf.RANKUP_FORMULA.apply(profile.getLevel());
        final ProgressBar progressBar = ProgressBar.builder()
            .bars(30)
            .prefix(RANKUP_BAR_PREFIX)
            .enablePrefix(true)
            .enableSuffix(true)
            .includePercentage(true)
            .build();

        player.sendActionBar(progressBar.getProgress(profile.getExperience(), expRequired).append(EVENT_5X));
    }

    @Override
    public void teleport(@NonNull String player, @Nullable UUID mineId, boolean staff) {
        final MiningUser miningUser = this.userCache.find(player);
        if (miningUser == null) {
            return;
        }

        if (mineId == null) { // Self mine
            final PrisonUserProfile profile = Objects.requireNonNull(this.profileService.getProfile(player));
            this.teleport(player, this.mineService.findOrCreate(profile), miningUser, staff);
            return;
        }

        final Optional<Mine> mineOpt = this.mineService.findMineById(mineId);
        if (mineOpt.isEmpty()) {
            return;
        }

        this.teleport(player, mineOpt.get(), miningUser, staff);
    }

    @Override
    public @Nullable Mine getCurrentMine(@NonNull String player) {
        final MiningUser miningUser = this.userCache.find(player);
        if (miningUser != null && miningUser.getCurrentMineId() != null) {
            return this.mineService.findMine(miningUser.getCurrentMineId());
        }

        return null;
    }

    @Internal
    private void teleport(String player, Mine mine, MiningUser miningUser, boolean staff) {
        final String themeId = mine.getThemeId();
        final MineTheme theme = MineTheme.findByName(themeId);
        if (theme == null) {
            return;
        }

        final Player bukkitPlayer = Bukkit.getPlayer(player);

        final MiningPosition spawn = theme.getStats().spawn();
        final String world = this.skinService.getFormattedWorldName(mine.getTheme());
        final String currentServerId = mine.getCurrentServerId(this.serverController); // Get or fetch a new server id
        if (currentServerId == null) { // Search a new server
            if (bukkitPlayer != null) {
                bukkitPlayer.sendMessage(miniMessage("<red>Não foi possível encontrar um servidor de minas disponível no momento. Tente "
                                                     + "novamente mais tarde."));
            }
            return;
        }

        final ServerLocation location = ServerLocation.builder()
            .x(spawn.x())
            .y(spawn.y())
            .z(spawn.z())
            .yaw(spawn.yaw())
            .pitch(spawn.pitch())
            .world(world)
            .serverId(currentServerId)
            .build();

        final GlobalPlayerService playerService = Services.load(GlobalPlayerService.class);
        final GlobalPlayer globalPLayer = playerService.getPlayer(player);
        if (globalPLayer == null) {
            if (bukkitPlayer != null) {
                bukkitPlayer.sendMessage(miniMessage("<red>Parece que seus dados não estão carregados? Recomendamos que relogue no servidor."));
            }
            return;
        }

        if (bukkitPlayer != null && MineServer.isMineServer()) {
            bukkitPlayer.setAllowFlight(true);
            bukkitPlayer.setFlying(true);
            bukkitPlayer.setNoclip(true);
        }

        mine.join(miningUser);
        this.userCache.insert(miningUser);

        globalPLayer.teleport(location);

        if (bukkitPlayer != null && staff && MineServer.isMineServer()) {
            final Plugin plugin = Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("mining"));
            mine.getMembers().forEach(memberPlayer -> bukkitPlayer.showPlayer(plugin, memberPlayer));
        }
    }
}
