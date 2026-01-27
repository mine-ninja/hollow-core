package io.github.minehollow.sdk.network.packet.impl.player;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.UUID;

public record SendModernMessageToPlayerPacket(
  @JsonProperty("pid") @JsonFormat(shape = JsonFormat.Shape.BINARY) UUID playerId,
  @JsonProperty("msg") String serializedMessageComponent,
  @JsonProperty("key") String key
) implements NetworkPacket {

    public static SendModernMessageToPlayerPacket create(UUID playerId, Component component, String key) {
        return new SendModernMessageToPlayerPacket(
          playerId,
          JSONComponentSerializer.json().serialize(component),
          key
        );
    }

    public static SendModernMessageToPlayerPacket create(UUID playerId, Component component) {
        return new SendModernMessageToPlayerPacket(
          playerId,
          JSONComponentSerializer.json().serialize(component),
          "unknown"
        );
    }

    @JsonIgnore
    public Component getMessage() {
        return JSONComponentSerializer.json().deserialize(serializedMessageComponent);
    }
}
