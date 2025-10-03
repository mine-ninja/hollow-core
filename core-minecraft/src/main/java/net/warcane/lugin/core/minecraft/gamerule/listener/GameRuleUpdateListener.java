package net.warcane.lugin.core.minecraft.gamerule.listener;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.network.packet.impl.gamerule.GameRuleUpdatePacket;
import net.warcane.lugin.core.network.packet.listener.PacketListener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for game rule updates from other servers in the network.
 */
@Slf4j
@RequiredArgsConstructor
public class GameRuleUpdateListener implements PacketListener<GameRuleUpdatePacket> {
    private final BukkitPlatform platform;
    
    @Override
    public void onReceivePacket(@NotNull GameRuleUpdatePacket packet, @NotNull Headers headers) {
        final String worldName = packet.worldName();
        final String ruleName = packet.ruleName();
        final Object value = packet.value();
        log.debug("Received game rule update from network: {} = {} for world {}", ruleName, value, worldName);
        // Update local cache via the manager's handleRemoteUpdate method
        platform.getGameRuleManager().handleRemoteUpdate(worldName, ruleName, value);
    }
}

