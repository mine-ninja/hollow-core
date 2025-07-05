package net.warcane.lugin.core.minecraft.util.team;

import com.google.common.collect.Maps;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class NametagHandler {

    private final Map<String, NametagTeam> teamsInCache = Maps.newHashMap();
    private final Map<String, NametagTeam> playersInCache = Maps.newHashMap();

    private final Plugin plugin;

    public NametagHandler(Plugin plugin) {
        this.plugin = plugin;
    }

    private void addPlayerToTeam(Player player, String prefix, String suffix, int sortPriority) {
        NametagTeam previous = getNametagTeam(player);

        if (previous != null && previous.isSimilar(prefix, suffix)) {
            plugin.getLogger().info(player + " already belongs to a similar team (" + previous.getName() + ")");
            return;
        }

        resetPlayer(player);

        NametagTeam joining = getNametagTeam(prefix, suffix);

        if (joining != null) {
            joining.addMember(player);
            plugin.getLogger().info("Using existing team for " + player.getName());
        } else {
            joining = new NametagTeam(prefix, suffix, sortPriority);
            joining.addMember(player);

            this.teamsInCache.put(joining.getName(), joining);

            addTeamPackets(joining);
            plugin.getLogger().info("Created team " + joining.getName() + ". Size: " + this.teamsInCache.size());
        }

        addPlayerToTeamPackets(joining, player);
        cachePlayer(player, joining);

        plugin.getLogger().info(player.getName() + " has been added to team " + joining.getName());
    }

    public NametagTeam resetPlayer(Player player) {
        return resetTeam(player, uncachePlayer(player));
    }

    private NametagTeam resetTeam(Player player, NametagTeam nametagTeam) {
        if (nametagTeam != null && nametagTeam.getMembers().remove(player.getName())) {
            boolean delete = removePlayerFromTeamPackets(nametagTeam, player.getName());

            plugin.getLogger().info(player.getName() + " was removed from " + nametagTeam.getName());

            if (delete) {
                removeTeamPackets(nametagTeam);
                this.teamsInCache.remove(nametagTeam.getName());
                plugin.getLogger().info("Team " + nametagTeam.getName() + " has been deleted. Size: " + this.teamsInCache.size());
            }
        }

        return nametagTeam;
    }

    public void setNametag(Player player, NametagTeam nametagTeam) {
        addPlayerToTeam(player, nametagTeam.getPrefix(), nametagTeam.getSuffix(), nametagTeam.getPriority());
    }

    public void setNametag(Player player, String prefix, String suffix, int sortPriority) {
        addPlayerToTeam(player, (prefix != null) ? prefix : "", (suffix != null) ? suffix : "", sortPriority);
    }

    private void cachePlayer(Player player, NametagTeam nametagTeam) {
        this.playersInCache.put(player.getName(), nametagTeam);
    }

    private NametagTeam uncachePlayer(Player player) {
        return this.playersInCache.remove(player.getName());
    }

    public NametagTeam getNametagTeam(Player player) {
        return this.playersInCache.get(player.getName());
    }

    private NametagTeam getNametagTeam(String prefix, String suffix) {
        for (NametagTeam nametagTeam : this.teamsInCache.values()) {
            if (nametagTeam.isSimilar(prefix, suffix)) {
                return nametagTeam;
            }
        }

        return null;
    }

    public void sendTeams(Player player) {
        teamsInCache.values()
          .forEach(nametagTeam -> new NametagWrapper(nametagTeam.getName(), nametagTeam.getPrefix(),
            nametagTeam.getSuffix(), 0, nametagTeam.getMembers()).send(player));
    }

    public void resetTeams() {
        teamsInCache.values().forEach(nametagTeam -> {
            removePlayerFromTeamPackets(nametagTeam, nametagTeam.getMembers());
            removeTeamPackets(nametagTeam);
        });
    }

    private void removeTeamPackets(NametagTeam nametagTeam) {
        new NametagWrapper(nametagTeam.getName(), nametagTeam.getPrefix(), nametagTeam.getSuffix(), 1,
          new ArrayList<>()).send();
    }

    private boolean removePlayerFromTeamPackets(NametagTeam nametagTeam, String... players) {
        return removePlayerFromTeamPackets(nametagTeam, Arrays.asList(players));
    }

    private boolean removePlayerFromTeamPackets(NametagTeam nametagTeam, List<String> players) {
        new NametagWrapper(nametagTeam.getName(), 4, players).send();

        nametagTeam.getMembers().removeAll(players);

        return nametagTeam.getMembers().isEmpty();
    }

    private void addTeamPackets(NametagTeam nametagTeam) {
        new NametagWrapper(nametagTeam.getName(), nametagTeam.getPrefix(), nametagTeam.getSuffix(), 0,
          nametagTeam.getMembers()).send();
    }

    private void addPlayerToTeamPackets(NametagTeam nametagTeam, Player player) {
        new NametagWrapper(nametagTeam.getName(), 3, Collections.singletonList(player.getName())).send();
    }

}
