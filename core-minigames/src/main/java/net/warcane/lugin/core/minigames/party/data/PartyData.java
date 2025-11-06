package net.warcane.lugin.core.minigames.party.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore
    public PartyData setLeader(PartyMember newLeader) {
        return new PartyData(this.partyId, newLeader, this.members, this.isOpen);
    }

    @JsonIgnore
    public PartyData setOpen() {
        return new PartyData(this.partyId, this.leader, this.members, true);
    }

    @JsonIgnore
    public PartyData setClose() {
        return new PartyData(this.partyId, this.leader, this.members, false);
    }

    @JsonIgnore
    public PartyData addMember(PartyMember member) {
        var newMembers = new HashSet<>(this.members);
        newMembers.add(member);
        return new PartyData(this.partyId, this.leader, newMembers, this.isOpen);
    }

    @JsonIgnore
    public PartyData removeMember(String removeTargetName) {
        var newMembers = new HashSet<>(this.members);
        newMembers.removeIf(member -> member.name().equalsIgnoreCase(removeTargetName));
        return new PartyData(this.partyId, this.leader, newMembers, this.isOpen);
    }

    @JsonIgnore
    public static PartyData fromJson(String json) {
        try {
            return objectMapper.readValue(json, PartyData.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonIgnore
    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
