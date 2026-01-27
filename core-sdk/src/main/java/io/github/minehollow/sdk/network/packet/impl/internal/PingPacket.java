package io.github.minehollow.sdk.network.packet.impl.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

public record PingPacket(@JsonProperty("t") long timestamp) implements NetworkPacket { }