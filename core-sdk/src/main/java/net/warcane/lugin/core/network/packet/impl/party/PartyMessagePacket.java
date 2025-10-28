package net.warcane.lugin.core.network.packet.impl.party;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.warcane.lugin.core.network.packet.NetworkPacket;

public record PartyMessagePacket(
    @JsonProperty("pid") String partyId,
    @JsonProperty("se") String serialization,
    @JsonProperty("el") boolean excludeLeader
) implements NetworkPacket {

    @JsonIgnore
    public static PartyMessagePacket create(String partyId, Component component) {
        return new PartyMessagePacket(
            partyId,
            JSONComponentSerializer.json().serialize(component),
            false
        );
    }

    @JsonIgnore
    public static PartyMessagePacket create(String partyId, Component component, boolean excludeLeader) {
        return new PartyMessagePacket(
            partyId,
            JSONComponentSerializer.json().serialize(component),
            excludeLeader
        );
    }

    @JsonIgnore
    public Component getMessage() {
        return JSONComponentSerializer.json().deserialize(serialization);
    }
}
