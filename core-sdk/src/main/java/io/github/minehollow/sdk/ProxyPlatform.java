package io.github.minehollow.sdk;

import io.github.minehollow.sdk.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;

public interface ProxyPlatform extends Platform {

    void registerServer(@NotNull String serverId, @NotNull HostAddress address);

    void unregisterServer(@NotNull String serverId);
}
