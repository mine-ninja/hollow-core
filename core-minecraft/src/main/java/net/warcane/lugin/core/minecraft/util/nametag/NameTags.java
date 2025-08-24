package net.warcane.lugin.core.minecraft.util.nametag;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams.*;

public class NameTags {
    private static final Map<String, NameTagTeam> playerTeams = new HashMap<>();

    public static void updateAllTags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            NameTagTeam team = playerTeams.get(player.getName());
            if (team != null) {
                setNameTag(player, team.prefix(), team.suffix(), team.priority(), team.color());
            }
        }
    }

    public static void setNameTag(@NotNull Player player, String prefix, String suffix, int priority) {
        setNameTag(player, prefix, suffix, priority , null);
    }

    public static void setNameTag(@NotNull Player player, String prefix, String suffix, int priority , @Nullable NamedTextColor color) {
        prefix = prefix != null ? prefix.substring(0, Math.min(prefix.length(), 16)) : "";
        suffix = suffix != null ? suffix.substring(0, Math.min(suffix.length(), 16)) : "";

        setNameTag(player, Component.text(prefix), Component.text(suffix), priority, color);
    }

    public static void setNameTag(@NotNull Player player, Component prefix, Component suffix, int priority, @Nullable NamedTextColor color) {
        try {
            String teamName = "LG_" + priority + "_" + player.getEntityId();

            NameTagTeam existingTeam = playerTeams.get(player.getName());
            if (existingTeam != null && !existingTeam.teamName().equals(teamName)) {
                removeNameTag(player);
            }

            ScoreBoardTeamInfo teamInfo = new ScoreBoardTeamInfo(
              Component.text(teamName),
              prefix,
              suffix,
              NameTagVisibility.ALWAYS,
              CollisionRule.NEVER,
              color,
              OptionData.NONE
            );

            TeamMode mode = existingTeam != null && existingTeam.teamName().equals(teamName)
              ? TeamMode.UPDATE
              : TeamMode.CREATE;

            WrapperPlayServerTeams packet = new WrapperPlayServerTeams(teamName, mode, teamInfo, player.getName());

            for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
                PacketEvents.getAPI().getPlayerManager().sendPacket(onlinePlayer, packet);

                final var onlinePlayerTeam = playerTeams.get(onlinePlayer.getName());
                if (onlinePlayerTeam != null) {
                    final var othersPacket = createTeamPacket(onlinePlayer);
                    if (othersPacket != null) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(player, othersPacket);
                    }
                }
            }

            playerTeams.put(player.getName(), new NameTagTeam(teamName, prefix, suffix, priority, color));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeNameTag(@NotNull Player player) {
        final var team = playerTeams.get(player.getName());
        if (team == null) {
            return;
        }

        final var packet = new WrapperPlayServerTeams(
          team.teamName,
          TeamMode.REMOVE,
          Optional.empty()
        );

        for (Player onlinePlayer : player.getServer().getOnlinePlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(onlinePlayer, packet);
        }

        playerTeams.remove(player.getName());
    }

    public static void clearPlayerTeams(@NotNull Player player) {
        removeNameTag(player);
    }

    public static void clearAllTeams() {
        for (String playerName : playerTeams.keySet()) {
            Player player = Bukkit.getPlayerExact(playerName);
            if (player != null && player.isOnline()) {
                removeNameTag(player);
            }
        }
        playerTeams.clear();
    }

    public static WrapperPlayServerTeams createTeamPacket(@NotNull Player player) {
        final var team = playerTeams.get(player.getName());
        if (team == null) return null;

        final var info = new ScoreBoardTeamInfo(
          Component.text(team.teamName),
          team.prefix,
          team.suffix,
          NameTagVisibility.ALWAYS,
          CollisionRule.NEVER,
          team.color,
          OptionData.NONE
        );


        return new WrapperPlayServerTeams(team.teamName, TeamMode.CREATE, info, player.getName());
    }

    record NameTagTeam(String teamName, Component prefix, Component suffix, int priority,
                       @Nullable NamedTextColor color) {
    }
}
