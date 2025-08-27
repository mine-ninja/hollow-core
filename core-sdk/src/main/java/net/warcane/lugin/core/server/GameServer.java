package net.warcane.lugin.core.server;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;

/**
 * Representa o um servidor dentro da nossa rede.
 *
 * @param serverId     ID do servidor
 * @param categoryType Categoria do servidor
 * @param serverPlayers  Contagem de jogadores no servidor
 * @param online       Indica se o servidor está online
 */
@JsonIgnoreProperties
public record GameServer(
  @JsonProperty("i") String serverId,
  @JsonProperty("c") ServerCategoryType categoryType,
  @JsonProperty("a") HostAddress hostAddress,
  @JsonProperty("p") ServerPlayers serverPlayers,
  @JsonProperty("o") boolean online
) {


    public GameServer withPlayerCount(ServerPlayers newServerPlayers) {
        return new GameServer(serverId, categoryType, hostAddress,
          newServerPlayers, online);
    }

    public GameServer withOnlineStatus(boolean newOnlineStatus) {
        return new GameServer(serverId, categoryType, hostAddress,
          serverPlayers, newOnlineStatus);
    }
}
