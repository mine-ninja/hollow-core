package net.warcane.lugin.core.network.packet.impl.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

public record PingPacket(@JsonProperty("t") long timestamp) implements NetworkPacket { }