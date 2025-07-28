package net.warcane.lugin.core.network.packet.impl.player;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.util.codec.BinaryComponentSerializer;

import java.util.UUID;

public record SendModernMessageToPlayerPacket(
  @JsonProperty("pid") @JsonFormat(shape = JsonFormat.Shape.BINARY) UUID playerId,
  @JsonProperty("msg") byte[] serializedMessageComponent
) implements NetworkPacket {

    public static SendModernMessageToPlayerPacket create(UUID playerId, Component component) {
        return new SendModernMessageToPlayerPacket(
          playerId,
          BinaryComponentSerializer.binary().serialize(component)
        );
    }

    public Component getMessage() {
        return BinaryComponentSerializer.binary().deserialize(serializedMessageComponent);
    }
}
