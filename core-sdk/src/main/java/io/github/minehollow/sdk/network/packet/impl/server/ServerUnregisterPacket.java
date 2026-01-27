package io.github.minehollow.sdk.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;

/**
 * Representa um pacote de desregistro de servidor.
 *
 * @param serverId ID do servidor a ser desregistrado
 */
public record ServerUnregisterPacket(
  @JsonProperty("i") String serverId
) implements NetworkPacket { }