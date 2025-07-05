package net.warcane.lugin.core;

import net.warcane.lugin.core.server.ServerPlayerCount;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;

public interface MinecraftServerPlatform extends Platform {

    @NotNull
    ServerCategoryType getServerCategoryType();

    @NotNull
    ServerPlayerCount getPlayerCount();

    @NotNull
    HostAddress getServerHostAddress();

    double[] getTps();
}
