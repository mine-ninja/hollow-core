package net.warcane.lugin.core.player.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Representa o estado do jogador na rede, incluindo o ID do jogador, nome e o ID do servidor atual.
 *
 * @param playerId        ID único do jogador
 * @param playerName      Nome do jogador
 * @param currentServerId ID do servidor atual em que o jogador está conectado (Ex: bw01)
 */
public record PlayerNetworkState(
  @JsonProperty("i") UUID playerId,
  @JsonProperty("n") String playerName,
  @JsonProperty("sid") String currentServerId
) {

    /**
     * Cria um novo estado de rede do jogador com o ID do jogador e nome.
     *
     * @param serverId ID do servidor atual do jogador.
     * @return Uma nova instância de PlayerNetworkState com o ID do jogador, nome e ID do servidor.
     */
    @Contract(pure = true)
    public PlayerNetworkState withCurrentServerId(@NotNull String serverId) {
        return new PlayerNetworkState(this.playerId, this.playerName, serverId);
    }
}
