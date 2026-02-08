package io.github.minehollow.lobby.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import io.github.minehollow.lobby.LobbyPlugin;
import io.github.minehollow.lobby.npc.NPCManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NPCClickListener extends PacketListenerAbstract {

    private final LobbyPlugin plugin;
    private final NPCManager npcManager;



    public NPCClickListener(@NotNull LobbyPlugin plugin, @NotNull NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
    }

    @Override
    public void onPacketReceive(@NotNull PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            var wrapper = new WrapperPlayClientInteractEntity(event);

            if (wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT ||
                wrapper.getAction() == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {

                var player = (Player) event.getPlayer();
                var entityId = wrapper.getEntityId();

                npcManager.handleInteraction(player, entityId);
            }
        }
    }
}