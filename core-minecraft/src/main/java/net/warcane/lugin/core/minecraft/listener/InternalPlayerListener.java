package net.warcane.lugin.core.minecraft.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.account.PlayerAccountService.AccountUnloadOptions;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.PlayerConnectedToServerPacket;
import net.warcane.lugin.core.network.packet.impl.player.PlayerDisconnectedFromServerPacket;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import static net.warcane.lugin.core.account.PlayerAccount.createDefaultAccount;
import static net.warcane.lugin.core.account.PlayerAccountService.AccountLoadOptions.withDefaultAccount;

@Slf4j
@RequiredArgsConstructor
public final class InternalPlayerListener implements Listener {

    private static final String FAILED_TO_LOAD_ERR_MSG = "§cSua conta está sendo revisada. Tente novamente em alguns minutos. (Código de erro: 1001)";

    private final BukkitPlatform platform;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();

        final var playerId = player.getUniqueId();
        final var name = player.getName();
        final var packet = new PlayerConnectedToServerPacket(playerId, currentServerId);
        platform.getNetworkClient().sendPacket(NetworkChannel.PLAYER_CONNECTION, packet);

        platform.getPlayerAccountService().loadPlayerAccount(
          playerId,
          withDefaultAccount(createDefaultAccount(playerId, name), true)
        ).whenComplete((playerAccount, error) -> {
            if (error != null) {
                log.error("Failed to load player account for {}: {}", player.getName(), error.getMessage(), error);
                this.syncKick(player);
                return;
            }

            log.debug("Player account loaded for {}: {}", player.getName(), playerAccount);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();

        final var packet = new PlayerDisconnectedFromServerPacket(player.getUniqueId(), currentServerId);
        platform.getNetworkClient().sendPacket(NetworkChannel.PLAYER_CONNECTION, packet);

        final var unloadOptions = new AccountUnloadOptions(false, true);
        platform.getPlayerAccountService().unloadPlayerAccount(player.getUniqueId(), unloadOptions).whenComplete((unloaded, error) -> {
            if (error != null) {
                log.error("Failed to unload player account for {}: {}", player.getName(), error.getMessage(), error);
            } else if (unloaded == null) {
                log.debug("Player account not found for {} during unload", player.getName());
            }
        });
    }

    private void syncKick(@NotNull Player player) {
        Tasks.runSync(() -> {
            if (player.isOnline()) {
                player.kickPlayer(InternalPlayerListener.FAILED_TO_LOAD_ERR_MSG);
            }
        });
    }
}
