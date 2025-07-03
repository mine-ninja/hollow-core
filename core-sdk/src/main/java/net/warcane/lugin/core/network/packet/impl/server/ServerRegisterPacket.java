package net.warcane.lugin.core.network.packet.impl.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.network.packet.NetworkPacket;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;

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
