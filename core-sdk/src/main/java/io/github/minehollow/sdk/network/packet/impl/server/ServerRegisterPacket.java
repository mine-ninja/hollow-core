package io.github.minehollow.sdk.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.network.packet.NetworkPacket;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.util.address.HostAddress;

/**
 * Representa o pacote de registro de um servidor na rede.
 *
 * @param serverId          ID do servidor
 * @param categoryType      Categoria do servidor
 * @param hostAddress Endereço do servidor
 */
public record ServerRegisterPacket(
  @JsonProperty("i") String serverId,
  @JsonProperty("c") ServerCategoryType categoryType,
  @JsonProperty("a") HostAddress hostAddress
) implements NetworkPacket {}
