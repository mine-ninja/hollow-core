package net.warcane.lugin.core.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.warcane.lugin.core.player.PlayerCount;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.address.HostAddress;

/**
 * Representa o um servidor dentro da nossa rede.
 *
 * @param serverId     ID do servidor
 * @param categoryType Categoria do servidor
 * @param playerCount  Contagem de jogadores no servidor
 * @param online       Indica se o servidor está online
 */
public record GameServer(
  @JsonProperty("i") String serverId,
  @JsonProperty("c") ServerCategoryType categoryType,
  @JsonProperty("a") HostAddress hostAddress,
  @JsonProperty("p") PlayerCount playerCount,
  @JsonProperty("o") boolean online
) {

    public GameServer withPlayerCount(PlayerCount newPlayerCount) {
        return new GameServer(serverId, categoryType, hostAddress,
          newPlayerCount, online);
    }

    public GameServer withOnlineStatus(boolean newOnlineStatus) {
        return new GameServer(serverId, categoryType, hostAddress,
          playerCount, newOnlineStatus);
    }
}
