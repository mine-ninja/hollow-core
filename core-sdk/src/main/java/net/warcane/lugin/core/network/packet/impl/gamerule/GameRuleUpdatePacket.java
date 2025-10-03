package net.warcane.lugin.core.network.packet.impl.gamerule;

import net.warcane.lugin.core.network.packet.NetworkPacket;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Packet sent when a custom game rule is updated.
 * Used for cross-server synchronization.
 * <p>
 * IMPORTANT: worldName can be NULL for global game rules!
 */
public record GameRuleUpdatePacket(@Nullable String worldName, @NotNull String ruleName, @Nullable Object value) implements NetworkPacket { }
