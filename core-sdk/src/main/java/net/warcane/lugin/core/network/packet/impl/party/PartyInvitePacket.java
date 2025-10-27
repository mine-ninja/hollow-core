package net.warcane.lugin.core.network.packet.impl.party;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.UUID;

public record PartyInvitePacket(
    @JsonProperty("pid") UUID playerId,
    @JsonProperty("msg") String msg

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
