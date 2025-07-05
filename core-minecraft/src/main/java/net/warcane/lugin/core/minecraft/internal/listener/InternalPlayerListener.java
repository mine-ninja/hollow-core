package net.warcane.lugin.core.minecraft.internal.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.group.PlayerGroup;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.event.PlayerAccountUpdateEvent;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.Tab;
import net.warcane.lugin.core.minecraft.util.team.NametagAPI;
import net.warcane.lugin.core.network.channel.NetworkChannel;
import net.warcane.lugin.core.network.packet.impl.player.PlayerConnectedToServerPacket;
import net.warcane.lugin.core.network.packet.impl.player.PlayerDisconnectedFromServerPacket;
import net.warcane.lugin.core.player.account.PlayerAccountService.AccountUnloadOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.warcane.lugin.core.minecraft.task.Tasks.runAsyncLater;
import static net.warcane.lugin.core.player.account.PlayerAccount.createDefaultAccount;
import static net.warcane.lugin.core.player.account.PlayerAccountService.AccountLoadOptions.withDefaultAccount;

@Slf4j
@RequiredArgsConstructor
public final class InternalPlayerListener implements Listener {

    private static final List<String> HEADER = List.of("§e§lLUGIN.COM.BR");
    private static final List<String> FOOTER = List.of("§aRanks, cosméticos e caixas em §c§lLUGIN.COM.BR");

    private static final Tab TAB = new Tab(HEADER, FOOTER);

    private static final String FAILED_TO_LOAD_ERR_MSG = "§cSua conta está sendo revisada. Tente novamente em alguns minutos. (Código de erro: 1001)";

    private final BukkitPlatform platform;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();

        final var playerId = player.getUniqueId();
        final var name = player.getName();


        platform.getPlayerAccountService().loadPlayerAccount(
          playerId,
          withDefaultAccount(createDefaultAccount(playerId, name), true)
        ).whenComplete((playerAccount, error) -> {
            if (error != null) {
                log.error("Failed to load player account for {}: {}", player.getName(), error.getMessage(), error);
                this.syncKick(player);
                return;
            }

            PlayerGroup group = playerAccount.currentSubscription().group();
            final var priority = group.getPowerLevel();
            final var groupPrefix = group.getPrefix();

            NametagAPI.getInstance().applyTag(player, groupPrefix + " ", "", priority);

            // Só envia o pacote de connect caso realmente carregue as informações dele.
            // caso o contrario ele vai ser kickado (como mostra no código acima).
            final var packet = new PlayerConnectedToServerPacket(playerId, currentServerId);
            platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);

            log.debug("Player account loaded for {}: {}", player.getName(), playerAccount);

            platform.getGameServerService().update(platform.getGameServer());
            platform.getPermissionInjector().injectPermissions(player);

            TAB.tick(player);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        final var currentServerId = platform.getId();

        // Envia o pacote de desconexão do jogador para o servidor, mesmo que não tenha a conta atualizada.
        final var packet = new PlayerDisconnectedFromServerPacket(player.getUniqueId(), currentServerId);
        platform.getNetworkClient().sendNetworkPacket(NetworkChannel.PLAYER_CONNECTION, packet);

        final var unloadOptions = new AccountUnloadOptions(false, true);
        platform.getPlayerAccountService().unloadPlayerAccount(player.getUniqueId(), unloadOptions).whenComplete((unloaded, error) -> {
            if (error != null) {
                log.error("Failed to unload player account for {}: {}", player.getName(), error.getMessage(), error);
            } else if (unloaded == null) {
                log.info("Player account not found for {} during unload", player.getName());
            } else {
                log.info("Player account unloaded for {}: {}", player.getName(), unloaded);
            }


            runAsyncLater(platform::updateServerInfo, 20);
        });
    }

    @EventHandler
    public void onGroupUpdate(PlayerAccountUpdateEvent event) {
        Player localPlayer = event.getLocalPlayer();
        if (localPlayer == null) return;

        PlayerGroup group = event.getPlayerAccount().currentSubscription().group();
        final var priority = group.getPowerLevel();
        final var groupPrefix = group.getPrefix();
        NametagAPI.getInstance().applyTag(localPlayer, groupPrefix + " ", "", priority);
    }

    private void syncKick(@NotNull Player player) {
        Tasks.runSync(() -> {
            if (player.isOnline()) {
                player.kickPlayer(InternalPlayerListener.FAILED_TO_LOAD_ERR_MSG);
            }
        });
    }
}
