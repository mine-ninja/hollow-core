package net.warcane.lugin.core;

import net.warcane.lugin.core.server.ServerPlayers;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.server.type.ServerSubCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;

public interface MinecraftServerPlatform extends Platform {

    @NotNull
    ServerCategoryType getServerCategoryType();
    
    @NotNull ServerSubCategoryType getServerSubCategoryType();

    @NotNull
    ServerPlayers getPlayerCount();

    @NotNull
    HostAddress getServerHostAddress();

    double[] getTps();
}
