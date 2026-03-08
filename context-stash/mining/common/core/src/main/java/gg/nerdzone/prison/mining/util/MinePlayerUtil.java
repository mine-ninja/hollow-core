/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.util;

import gg.nerdzone.prison.mining.model.user.Mine;
import gg.nerdzone.prison.mining.services.MiningUserService;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class MinePlayerUtil {

    private final PotionEffect NIGHT_VISION = new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 2, false, false);

    public void setMineAttributes(@Nullable Player player, Mine mine, Plugin plugin) {
        if (player == null) {
            return;
        }

        final boolean inMine = mine != null;
        player.setAllowFlight(inMine);
        player.setNoclip(inMine);

        if (inMine) {
            player.addPotionEffect(NIGHT_VISION);
        }

        player.getWorld().getPlayers().forEach(onlinePlayer -> {
            if (inMine && !(mine.isMember(onlinePlayer.getName())) && !onlinePlayer.hasMetadata("vanished")) {
                onlinePlayer.hideEntity(plugin, player);
                player.hideEntity(plugin, onlinePlayer);
            }
        });

        // Staff handling (Staff can see everyone but members can only see other members)
        if (mine != null) {
            final boolean playerStaff = player.hasMetadata("vanished");
            mine.getMembers().forEach(member -> {
                final boolean memberStaff = member.hasMetadata("vanished");
                if (playerStaff && !memberStaff) {
                    member.showEntity(plugin, player);
                } else if (!playerStaff && memberStaff) {
                    player.hideEntity(plugin, member);
                }
            });
        }
    }

    public void setMineAttributes(@Nullable Player player, @NonNull MiningUserService userService, @NonNull Plugin plugin) {
        if (player == null) {
            return;
        }

        final Mine currentMine = userService.getCurrentMine(player.getName());
        setMineAttributes(player, currentMine, plugin);
    }

    public void removeMineAttributes(@Nullable Player player) {
        if (player == null) {
            return;
        }

        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        player.setNoclip(false);
    }
}
