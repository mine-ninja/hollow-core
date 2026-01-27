package io.github.minehollow.sdk;

import io.github.minehollow.sdk.server.ServerPlayers;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.server.type.ServerSubCategoryType;
import io.github.minehollow.sdk.util.address.HostAddress;
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
