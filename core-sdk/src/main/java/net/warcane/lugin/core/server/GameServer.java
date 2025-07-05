package net.warcane.lugin.core.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;

/**
 * Representa o um servidor dentro da nossa rede.
 *
 * @param serverId     ID do servidor
 * @param categoryType Categoria do servidor
 * @param serverPlayerCount  Contagem de jogadores no servidor
 * @param online       Indica se o servidor está online
 */
public record GameServer(
  @JsonProperty("i") String serverId,
  @JsonProperty("c") ServerCategoryType categoryType,
  @JsonProperty("a") HostAddress hostAddress,
  @JsonProperty("p") ServerPlayerCount serverPlayerCount,
  @JsonProperty("o") boolean online
) {

    public GameServer withPlayerCount(ServerPlayerCount newServerPlayerCount) {
        return new GameServer(serverId, categoryType, hostAddress,
          newServerPlayerCount, online);
    }

    public GameServer withOnlineStatus(boolean newOnlineStatus) {
        return new GameServer(serverId, categoryType, hostAddress,
          serverPlayerCount, newOnlineStatus);
    }
}
