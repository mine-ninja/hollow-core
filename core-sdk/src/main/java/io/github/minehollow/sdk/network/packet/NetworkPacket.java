package io.github.minehollow.sdk.network.packet;

import java.io.Serializable;

public interface NetworkPacket extends Serializable {

    static int idOf(Class<? extends NetworkPacket> packetClass) {
        return PacketMappings.PACKET_ID_BY_CLASS.getInt(packetClass);
    }

    static Class<? extends NetworkPacket> classOf(int id) {
        return PacketMappings.PACKET_CLASS_BY_ID.get(id);
    }
}
