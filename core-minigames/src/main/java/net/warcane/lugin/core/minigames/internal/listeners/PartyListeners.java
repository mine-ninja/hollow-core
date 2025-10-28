package net.warcane.lugin.core.minigames.internal.listeners;

import net.warcane.lugin.core.minecraft.event.account.PlayerAccountLoadEvent;
import net.warcane.lugin.core.minecraft.util.LocationUtil;
import net.warcane.lugin.core.minigames.MinigamesPlatform;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PartyListeners implements Listener {

    private final MinigamesPlatform platform;

    public PartyListeners(MinigamesPlatform platform) {
        this.platform = platform;
    }

    @EventHandler
    public void onJoinEvent(PlayerAccountLoadEvent event) {
        var playerAccount = event.getLoadedAccount();
        var party = platform.getPartyService().findPartyPlayer(playerAccount.playerName());

        if (party == null) {
            return;
        }

        if (!party.leader().name().equalsIgnoreCase(playerAccount.playerName())) {
            return;
        }

        var remoteLocation = LocationUtil.convertToRemoteLocation(Bukkit.getPlayer(playerAccount.playerName()).getLocation());
        var members = party.members();
        for (var member : members) {
            if (Bukkit.getPlayer(member.uniqueId()) == null) {
                continue;
            }

//            BukkitPlatform.getInstance().getTeleportManager().teleport(member.name(), remoteLocation, ConnectionReason.TELEPORT, null);
        }
    }

    @EventHandler
    public void onLeaveEvent(PlayerQuitEvent event) {
        //TODO: Set party leave logic
    }

    @EventHandler
    public void onChangerWorldEvent(PlayerChangedWorldEvent event) {
        var player = event.getPlayer();
        var party = platform.getPartyService().findPartyPlayer(player.getName());

        if (party == null) {
            return;
        }

        if (!party.leader().name().equalsIgnoreCase(player.getName())) {
            return;
        }

        var leaderLocation = player.getLocation();
        var members = party.members();
        for (var member : members) {
            var playerMember = Bukkit.getPlayer(member.uniqueId());
            if (playerMember != null && !playerMember.getLocation().getWorld().getName().equalsIgnoreCase(leaderLocation.getWorld().getName())) {
                playerMember.teleport(leaderLocation);
                continue;
            }

//            BukkitPlatform.getInstance().getTeleportManager().teleport(member.name(), remoteLocation, ConnectionReason.TELEPORT, null);
        }
    }
}
