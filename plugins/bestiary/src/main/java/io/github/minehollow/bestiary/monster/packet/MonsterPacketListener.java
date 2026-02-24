package io.github.minehollow.bestiary.monster.packet;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import io.github.minehollow.bestiary.monster.MonsterManager;

public class MonsterPacketListener extends PacketListenerAbstract {

    private final MonsterManager monsterManager;

    public MonsterPacketListener(MonsterManager monsterManager) {
        this.monsterManager = monsterManager;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(event);
            var active = monsterManager.getActive(packet.getUUID().orElse(null));

            if (active != null) {
                event.getTasksAfterSend().add(() -> event.getUser().sendPacket(active.hologram().getPassengersPacket()));
            }
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_PASSENGERS) {
            WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(event);
            var active = monsterManager.getActive(packet.getEntityId());

            if (active != null) {
                int holoId = active.hologram().getEntityId();
                boolean contains = false;
                for (int id : packet.getPassengers()) {
                    if (id == holoId) {
                        contains = true;
                    }
                }

                if (!contains) {
                    int[] newPassengers = new int[packet.getPassengers().length + 1];
                    System.arraycopy(packet.getPassengers(), 0, newPassengers, 0, packet.getPassengers().length);
                    newPassengers[newPassengers.length - 1] = holoId;
                    packet.setPassengers(newPassengers);
                    event.markForReEncode(true);
                }
            }
        }
    }
}