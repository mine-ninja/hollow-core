package net.warcane.lugin.core;

import net.warcane.lugin.core.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;

public interface ProxyPlatform extends Platform {

    void registerServer(@NotNull String serverId, @NotNull HostAddress address);

    void unregisterServer(@NotNull String serverId);
}
