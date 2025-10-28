package net.warcane.lugin.core.network.packet.impl.party;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

import java.util.List;

public record PartyDeletedPacket(
    @JsonProperty("mn") List<String> memberNames,
    @JsonProperty("ms") String message
) implements NetworkPacket { }
