package net.warcane.lugin.core.minigames.party.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashSet;

public record PartyData(
    @JsonProperty("pi") String partyId,
    @JsonProperty("ln") PartyMember leader,
    @JsonProperty("mn") HashSet<PartyMember> members,
    @JsonProperty("io") boolean isOpen
) {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static PartyData fromJson(String json) {
        try {
            return objectMapper.readValue(json, PartyData.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
