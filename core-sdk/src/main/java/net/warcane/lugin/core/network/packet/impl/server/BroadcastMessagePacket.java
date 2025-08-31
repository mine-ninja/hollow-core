package net.warcane.lugin.core.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.warcane.lugin.core.network.packet.NetworkPacket;

public record BroadcastMessagePacket(
  @JsonProperty("msg") String serializedComponentMessage,
  @JsonProperty("key") String key
) implements NetworkPacket {

    public static BroadcastMessagePacket create(String message, String key) {
        return new BroadcastMessagePacket(JSONComponentSerializer.json().serialize(Component.text(message)), key);
    }

    public static BroadcastMessagePacket create(Component message) {
        return new BroadcastMessagePacket(JSONComponentSerializer.json().serialize(message), "unknown");
    }

    @JsonIgnore
    public Component getMessage() {
        return JSONComponentSerializer.json().deserialize(serializedComponentMessage);
    }
}
