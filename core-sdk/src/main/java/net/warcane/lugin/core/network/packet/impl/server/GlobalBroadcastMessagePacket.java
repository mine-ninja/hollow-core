package net.warcane.lugin.core.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.warcane.lugin.core.network.packet.NetworkPacket;

public record GlobalBroadcastMessagePacket(
  @JsonProperty("msg") String serializedComponentMessage,
  @JsonProperty("key") String key
) implements NetworkPacket {

    public static GlobalBroadcastMessagePacket create(String message, String key) {
        return new GlobalBroadcastMessagePacket(JSONComponentSerializer.json().serialize(Component.text(message)), key);
    }

    public static GlobalBroadcastMessagePacket create(Component message) {
        return new GlobalBroadcastMessagePacket(JSONComponentSerializer.json().serialize(message), "unknown");
    }

    public static GlobalBroadcastMessagePacket create(Component message, String key) {
        return new GlobalBroadcastMessagePacket(JSONComponentSerializer.json().serialize(message), key);
    }

    @JsonIgnore
    public Component getMessage() {
        return JSONComponentSerializer.json().deserialize(serializedComponentMessage);
    }
}
