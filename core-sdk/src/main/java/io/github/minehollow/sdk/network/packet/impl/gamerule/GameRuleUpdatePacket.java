package io.github.minehollow.sdk.network.packet.impl.gamerule;

import io.github.minehollow.sdk.network.packet.NetworkPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Network packet for cross-server game rule synchronization.
 * <p>
 * Sent when a game rule is updated to notify all servers in the network.
 *
 * @param worldName the world name, or null for global rules
 * @param ruleName the rule name
 * @param value the new value, or null for removal
 */
public record GameRuleUpdatePacket(
    @Nullable String worldName,
    @NotNull String ruleName,
    @Nullable Object value
) implements NetworkPacket { }
