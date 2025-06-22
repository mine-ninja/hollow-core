package net.warcane.lugin.core.minecraft.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GameProfileUtil {

    public static GameProfile createGameProfile(@NotNull String name) {
        return new GameProfile(java.util.UUID.randomUUID(), name);
    }

    public static GameProfile getGameProfile(@NotNull Player player){
        return ((CraftPlayer)player).getProfile();
    }

    public static PropertyMap getPropertyMap(@NotNull Player player) {
        return getGameProfile(player).getProperties();
    }
}
