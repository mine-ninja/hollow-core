package net.warcane.lugin.core.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.util.codec.BinaryComponentSerializer;

public record BroadcastMessagePacket(
  @JsonProperty("msg") byte[] serializedComponentMessage
) implements NetworkPacket {

    public static BroadcastMessagePacket create(Component message) {
        return new BroadcastMessagePacket(BinaryComponentSerializer.serializeUnchecked(message));
    }

    public Component getMessage() {
        return BinaryComponentSerializer.deserializeUnchecked(serializedComponentMessage);
    }
}
