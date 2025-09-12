package net.warcane.lugin.core.minecraft.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

public class PlayerUtil {

    public static Player getPlayerByName(@NotNull String playerName){
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                return player;
            }
        }
        return null; // Player not found
    }

    public static boolean isCracked(Player player) {
        PlayerProfile profile = player.getPlayerProfile();

        Optional<ProfileProperty> textures = profile.getProperties().stream()
            .filter(p -> "textures".equalsIgnoreCase(p.getName()))
            .findFirst();

        if (textures.isEmpty())
            return true;

        ProfileProperty prop = textures.get();
        String signature = prop.getSignature();

        return signature == null || signature.isEmpty();
    }

    public static boolean isOnline(Player player) {
        if (player == null) return false;
        Player found = Bukkit.getPlayer(player.getUniqueId());
        return found != null && found.isOnline();
    }
}
