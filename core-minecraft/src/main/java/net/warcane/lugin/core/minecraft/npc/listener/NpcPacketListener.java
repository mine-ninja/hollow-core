package net.warcane.lugin.core.minecraft.npc.listener;

import gnu.trove.map.TObjectLongMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;
import net.warcane.lugin.core.minecraft.npc.Npc;
import net.warcane.lugin.core.minecraft.npc.NpcManager;
import net.warcane.lugin.core.minecraft.npc.interact.NpcInteractListener;
import net.warcane.lugin.core.minecraft.npc.interact.NpcInteractListener.ClickType;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.UtilReflection;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


@Slf4j
@RequiredArgsConstructor
public class NpcPacketListener implements Listener {

    private final NpcManager npcManager;
    private final TObjectLongMap<UUID> cooldownMap = new TObjectLongHashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        this.injectNettyListener(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        cooldownMap.remove(event.getPlayer().getUniqueId());
    }

    public boolean isInCooldown(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        long cooldownTime = cooldownMap.get(playerId);
        return cooldownTime > System.currentTimeMillis();
    }

    public void setCooldown(@NotNull Player player, long cooldownMillis) {
        UUID playerId = player.getUniqueId();
        cooldownMap.put(playerId, System.currentTimeMillis() + cooldownMillis);
    }

    public void injectNettyListener(@NotNull Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().playerConnection.networkManager.channel;
        ChannelPipeline pipeline = channel.pipeline();
        if (pipeline == null) return;

        log.info("Injecting Netty listener for player: {}", player.getName());
        try {
            pipeline.addBefore("packet_handler", "npc-duplex-handler", new CustomDuplexHandler(player));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Um adapter simples para lidar com pacotes de interação com NPCs.
     */
    @ApiStatus.Internal
    @RequiredArgsConstructor
    class CustomDuplexHandler extends ChannelInboundHandlerAdapter {

        private final Player player;

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            super.channelRead(ctx, msg);

            if (!(msg instanceof PacketPlayInUseEntity packet)) return;

            var useAction = packet.a();
            int entityId = UtilReflection.getFieldValue(packet, "a");
            Npc npc = npcManager.getNpc(entityId);
            if (npc == null || isInCooldown(player)) {
                return;
            }
            if (player.getLocation().distanceSquared(npc.getLocation()) > 36.0) {
                return;
            }

            NpcInteractListener interactListener = npc.getInteractListener();
            if (interactListener == null) {
                return;
            }

            Runnable action = null;
            if (useAction == PacketPlayInUseEntity.EnumEntityUseAction.INTERACT) {
                action = () -> interactListener.handlePlayerInteract(npc, player, ClickType.RIGHT_CLICK);
            } else if (useAction == PacketPlayInUseEntity.EnumEntityUseAction.ATTACK) {
                action = () -> interactListener.handlePlayerInteract(npc, player, ClickType.LEFT_CLICK);
            }

            if (action == null) {
                return;
            }

            Tasks.runSync(action);
            setCooldown(player, 300); // 1 segundo de cooldown
        }
    }
}
