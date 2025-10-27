package net.warcane.lugin.core.minigames.party.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.UUID;

public record PartyMember(
    @JsonProperty("n") String name,
    @JsonProperty("ui") UUID uniqueId
) implements Serializable { }
