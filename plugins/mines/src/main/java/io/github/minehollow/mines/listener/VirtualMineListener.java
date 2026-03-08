package io.github.minehollow.mines.listener;

import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.instance.MineInstance;
import io.github.minehollow.mines.service.VirtualMineService;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

public final class VirtualMineListener implements Listener {

    private final MinesPlugin plugin;
    private final VirtualMineService mineService;
    private final Set<UUID> mineFlightPlayers = ConcurrentHashMap.newKeySet();

    public VirtualMineListener(@NotNull MinesPlugin plugin, @NotNull VirtualMineService mineService) {
        this.plugin = plugin;
        this.mineService = mineService;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getWorld() == null || event.getTo().getWorld() == null) {
            return;
        }

        final Player player = event.getPlayer();
        MineInstance instance = this.mineService.findByPlayer(player.getUniqueId());

        // Auto-create/enter a mine when the player steps inside a global mine area.
        if (instance == null && this.hasBlockPositionChange(event)) {
            instance = this.mineService.autoEnterByLocation(player, event.getTo());
        }

        this.updateMineFlight(player);

        if (this.hasBlockPositionChange(event)) {
            this.syncVisibilityFor(player);
        }

        if (!this.hasChunkChange(event)) {
            return;
        }

        if (instance == null) {
            return;
        }

        this.mineService.getRenderer().sendChunkOverlay(
            player,
            instance,
            event.getTo().getBlockX() >> 4,
            event.getTo().getBlockZ() >> 4
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final MineInstance instance = this.mineService.findByPlayer(player.getUniqueId());
        if (instance != null) {
            this.mineService.getRenderer().sendNearChunks(player, instance);
        }

        this.updateMineFlight(player);
        this.syncVisibilityFor(player);
    }

    @EventHandler
    public void onPlayerWorldChange(PlayerChangedWorldEvent event) {
        final Player player = event.getPlayer();
        final MineInstance instance = this.mineService.findByPlayer(player.getUniqueId());
        if (instance != null) {
            this.mineService.getRenderer().sendNearChunks(player, instance);
        }

        this.updateMineFlight(player);
        this.syncVisibilityFor(player);
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        this.updateMineFlight(player);
        this.syncVisibilityFor(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final MineInstance mine = this.mineService.findByPlayer(player.getUniqueId());
        if (mine == null) {
            this.clearMineFlightState(player);
            return;
        }

        if (mine.getOwnerId().equals(player.getUniqueId())) {
            for (UUID memberId : mine.getMembers()) {
                if (memberId.equals(player.getUniqueId())) {
                    continue;
                }

                final Player member = Bukkit.getPlayer(memberId);
                if (member == null || !member.isOnline()) {
                    continue;
                }

                this.mineService.getRenderer().restoreNearChunks(member, mine);
                this.disableMineFlight(member);
                this.syncVisibilityFor(member);
            }

            this.mineService.getInstanceManager().disband(player.getUniqueId());
            this.clearMineFlightState(player);
            return;
        }

        this.mineService.getInstanceManager().leave(player.getUniqueId());
        this.clearMineFlightState(player);
    }

    private void updateMineFlight(@NotNull Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        final MineInstance instance = this.mineService.findByPlayer(player.getUniqueId());
        if (instance == null) {
            this.disableMineFlight(player);
            return;
        }

        final boolean inMineWorld = this.mineService.getRenderer().isMineWorld(player.getWorld());
        final boolean inGlobalArea = inMineWorld && instance.getDefinition().getGlobalArea().contains(
            player.getLocation().getBlockX(),
            player.getLocation().getBlockY(),
            player.getLocation().getBlockZ()
        );

        if (inGlobalArea) {
            this.enableMineFlight(player);
        } else {
            this.disableMineFlight(player);
        }
    }

    private void syncVisibilityFor(@NotNull Player player) {
        if (!player.isOnline()) {
            return;
        }

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            if (this.shouldHideBetween(player, other)) {
                player.hidePlayer(this.plugin, other);
                other.hidePlayer(this.plugin, player);
                continue;
            }

            player.showPlayer(this.plugin, other);
            other.showPlayer(this.plugin, player);
        }
    }

    private boolean shouldHideBetween(@NotNull Player first, @NotNull Player second) {
        final MineInstance firstInstance = this.mineService.findByPlayer(first.getUniqueId());
        final MineInstance secondInstance = this.mineService.findByPlayer(second.getUniqueId());

        // Both without a mine instance — never hide.
        if (firstInstance == null && secondInstance == null) {
            return false;
        }

        // Only hide when both players are physically inside their mine's global area.
        final boolean firstInArea = firstInstance != null
            && this.mineService.getRenderer().isMineWorld(first.getWorld())
            && firstInstance.getDefinition().getGlobalArea().contains(
                first.getLocation().getBlockX(),
                first.getLocation().getBlockY(),
                first.getLocation().getBlockZ()
            );

        final boolean secondInArea = secondInstance != null
            && this.mineService.getRenderer().isMineWorld(second.getWorld())
            && secondInstance.getDefinition().getGlobalArea().contains(
                second.getLocation().getBlockX(),
                second.getLocation().getBlockY(),
                second.getLocation().getBlockZ()
            );

        // If either player is outside their mine area, never hide.
        if (!firstInArea || !secondInArea) {
            return false;
        }

        // Both are inside their mine areas — hide only if they belong to different mines.
        return !firstInstance.getOwnerId().equals(secondInstance.getOwnerId());
    }

    private void enableMineFlight(@NotNull Player player) {
        if (!player.getAllowFlight()) {
            this.mineFlightPlayers.add(player.getUniqueId());
            player.setAllowFlight(true);
        }
    }

    private void disableMineFlight(@NotNull Player player) {
        if (!this.mineFlightPlayers.remove(player.getUniqueId())) {
            return;
        }

        if (player.isFlying()) {
            player.setFlying(false);
        }
        player.setAllowFlight(false);
    }

    private void clearMineFlightState(@NotNull Player player) {
        this.mineFlightPlayers.remove(player.getUniqueId());
    }

    private boolean hasChunkChange(@NotNull PlayerMoveEvent event) {
        return (event.getFrom().getBlockX() >> 4) != (event.getTo().getBlockX() >> 4)
            || (event.getFrom().getBlockZ() >> 4) != (event.getTo().getBlockZ() >> 4)
            || !event.getFrom().getWorld().equals(event.getTo().getWorld());
    }

    private boolean hasBlockPositionChange(@NotNull PlayerMoveEvent event) {
        return event.getFrom().getBlockX() != event.getTo().getBlockX()
            || event.getFrom().getBlockY() != event.getTo().getBlockY()
            || event.getFrom().getBlockZ() != event.getTo().getBlockZ()
            || !event.getFrom().getWorld().equals(event.getTo().getWorld());
    }
}
