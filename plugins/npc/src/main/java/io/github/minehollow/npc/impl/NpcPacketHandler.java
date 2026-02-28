package io.github.minehollow.npc.impl;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.github.minehollow.npc.api.Npc;
import io.github.minehollow.npc.api.NpcAction;
import io.github.minehollow.npc.api.NpcClickEvent;
import io.github.minehollow.npc.api.NpcClickType;
import io.github.minehollow.minecraft.task.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Listens for INTERACT_ENTITY packets and resolves them to NPC clicks.
 * Includes a per-player cooldown to prevent spam.
 */
public class NpcPacketHandler extends PacketListenerAbstract {

    private static final long CLICK_COOLDOWN_MS = 250;

    private final NpcRegistryImpl registry;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public NpcPacketHandler(@NotNull NpcRegistryImpl registry) {
        this.registry = registry;
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        var wrapper = new WrapperPlayClientInteractEntity(event);
        int entityId = wrapper.getEntityId();

        Npc npc = registry.getByEntityId(entityId);
        if (npc == null) return;

        Player player = (Player) event.getPlayer();
        if (player == null) return;

        // Cooldown check
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && now - last < CLICK_COOLDOWN_MS) return;
        cooldowns.put(uuid, now);

        // Determine click type
        NpcClickType clickType = resolveClickType(wrapper, player);

        // Check if NPC accepts this click type
        NpcClickType accepted = npc.getClickType();
        if (accepted != NpcClickType.ANY && accepted != clickType) return;

        // Fire event + execute actions on the main thread
        Tasks.runSync(() -> {
            NpcClickEvent clickEvent = new NpcClickEvent(npc, player, clickType);
            Bukkit.getPluginManager().callEvent(clickEvent);

            for (NpcAction action : npc.getActions()) {
                action.execute(player, npc);
            }
        });
    }

    private @NotNull NpcClickType resolveClickType(@NotNull WrapperPlayClientInteractEntity wrapper,
                                                    @NotNull Player player) {
        boolean sneaking = player.isSneaking();
        return switch (wrapper.getAction()) {
            case ATTACK -> sneaking ? NpcClickType.SHIFT_LEFT : NpcClickType.LEFT;
            case INTERACT, INTERACT_AT -> sneaking ? NpcClickType.SHIFT_RIGHT : NpcClickType.RIGHT;
        };
    }
}

