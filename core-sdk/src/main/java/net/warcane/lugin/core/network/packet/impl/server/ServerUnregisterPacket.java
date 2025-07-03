package net.warcane.lugin.core.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;

/**
 * Representa um pacote de desregistro de servidor.
 *
 * @param serverId ID do servidor a ser desregistrado
 */
public record ServerUnregisterPacket(
  @JsonProperty("i") String serverId
) implements NetworkPacket { }