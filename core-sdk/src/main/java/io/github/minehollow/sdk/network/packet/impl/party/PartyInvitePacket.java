package io.github.minehollow.sdk.network.packet.impl.party;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

import java.util.UUID;

public record PartyInvitePacket(
    @JsonProperty("pid") UUID playerId,
    String msg

) implements NetworkPacket {

    @JsonIgnore
    public static PartyInvitePacket create(UUID playerId, Component component) {
        return new PartyInvitePacket(
            playerId,
            JSONComponentSerializer.json().serialize(component)
        );
    }

    @JsonIgnore
    public Component getMessage() {
        return JSONComponentSerializer.json().deserialize(msg);
    }
}
