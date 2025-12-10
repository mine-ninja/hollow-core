package net.warcane.lugin.core.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.server.type.ServerCategoryType;

public record ServerCategoryMessagePacket(
    @JsonProperty("msg") String serializedComponentMessage,
    @JsonProperty("key") String key,
    @JsonProperty("ctg") String serverCategory
    ) implements NetworkPacket {

    public static ServerCategoryMessagePacket create(String message, String key, ServerCategoryType serverCategory) {
        return new ServerCategoryMessagePacket(JSONComponentSerializer.json().serialize(Component.text(message)), key, serverCategory.name());
    }

    public static ServerCategoryMessagePacket create(Component message, ServerCategoryType serverCategory) {
        return new ServerCategoryMessagePacket(JSONComponentSerializer.json().serialize(message), "unknown", serverCategory.name());
    }

    public static ServerCategoryMessagePacket create(Component message, String key, ServerCategoryType serverCategory) {
        return new ServerCategoryMessagePacket(JSONComponentSerializer.json().serialize(message), key, serverCategory.name());
    }

    @JsonIgnore
    public boolean isForCategory(ServerCategoryType category) {
        return this.serverCategory.equalsIgnoreCase(category.name());
    }

    @JsonIgnore
    public Component getMessage() {
        return JSONComponentSerializer.json().deserialize(serializedComponentMessage);
    }
}
