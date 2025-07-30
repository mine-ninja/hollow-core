package net.warcane.lugin.core.minecraft.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerUtil {

    public static Player getPlayerByName(@NotNull String playerName){
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return player;
            }
        }
        return null; // Player not found
    }
}
