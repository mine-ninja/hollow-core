package net.warcane.lugin.core.network.packet;

import java.io.Serializable;

public interface NetworkPacket extends Serializable {

    static int idOf(Class<? extends NetworkPacket> packetClass) {
        throw new UnsupportedOperationException("Method not implemented yet");
    }

    static Class<? extends NetworkPacket> classOf(int id) {
        throw new UnsupportedOperationException("Method not implemented yet");
    }
}
