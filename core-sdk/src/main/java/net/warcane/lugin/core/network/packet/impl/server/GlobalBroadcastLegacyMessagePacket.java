package net.warcane.lugin.core.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.warcane.lugin.core.network.packet.NetworkPacket;

public record GlobalBroadcastLegacyMessagePacket(
    @JsonProperty("msg") String serializedComponentMessage,
    @JsonProperty("key") String key
) implements NetworkPacket {

    public static GlobalBroadcastLegacyMessagePacket create(String message, String key) {
        return new GlobalBroadcastLegacyMessagePacket(JSONComponentSerializer.json().serialize(Component.text(message)), key);
    }

    public static GlobalBroadcastLegacyMessagePacket create(Component message) {
        return new GlobalBroadcastLegacyMessagePacket(JSONComponentSerializer.json().serialize(message), "unknown");
    }

    public static GlobalBroadcastLegacyMessagePacket create(Component message, String key) {
        return new GlobalBroadcastLegacyMessagePacket(JSONComponentSerializer.json().serialize(message), key);
    }

    @JsonIgnore
    public Component getMessage() {
        return JSONComponentSerializer.json().deserialize(serializedComponentMessage);
    }
}
