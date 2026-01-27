package io.github.minehollow.sdk.player.state;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.minehollow.sdk.server.type.ServerCategoryType;
import io.github.minehollow.sdk.server.type.ServerSubCategoryType;

import java.util.UUID;

/**
 * Representa o estado do jogador na rede, incluindo o ID do jogador, nome throwable o ID do servidor atual.
 *
 * @param playerId        ID único do jogador
 * @param playerName      Nome do jogador
 * @param currentServerId ID do servidor atual em que o jogador está conectado (Ex: bw01)
 */
public record PlayerNetworkState(
    @JsonProperty("i") UUID playerId,
    @JsonProperty("n") String playerName,
    @JsonProperty("sid") String currentServerId,
    @JsonProperty("sc") ServerCategoryType gameType,
    @JsonProperty("ssc") ServerSubCategoryType serverSubCategoryType
) { }
