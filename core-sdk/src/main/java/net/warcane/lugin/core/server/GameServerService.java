package net.warcane.lugin.core.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.util.JsonUtil;
import net.warcane.lugin.core.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representa o serviço de gerenciamento de servidores de jogo na rede.
 * Fornece métodos para registrar, consultar e atualizar servidores.
 */
@Slf4j
@RequiredArgsConstructor
public class GameServerService {

    // Chave para armazenar o estado do servidor no Redis
    private static final String KEY = "gs";
    // Chave para o índice de categorias de servidores
    private static final String CATEGORY_IDX_KEY = "gsc:";

    /**
     * Conector Redis para persistência de dados do estado do servidor
     */
    private final RedisConnector connector;

    public int getTotalPlayerCount() {
        return queryAllServersInNetwork()
          .stream()
          .map(GameServer::serverPlayerCount)
          .mapToInt(ServerPlayerCount::online)
          .sum();
    }

    public int getPlayerCountForServerCategory(@NotNull ServerCategoryType categoryType) {
        return queryServersByCategoryType(categoryType)
          .stream()
          .map(GameServer::serverPlayerCount)
          .mapToInt(ServerPlayerCount::online)
          .sum();
    }

    @Nullable
    public GameServer getByAddress(@NotNull HostAddress hostAddress) {
        return connector.supplyFromJedis(jedis -> {
            final var rawData = jedis.hvals(KEY);
            return rawData.stream()
              .map(data -> JsonUtil.fromJson(data, GameServer.class))
              .filter(Objects::nonNull)
              .filter(server -> server.hostAddress().equals(hostAddress))
              .findFirst()
              .orElse(null);
        });
    }

    @Nullable
    public GameServer getById(@NotNull String serverId) {
        return connector.supplyFromJedis(jedis -> {
            final var rawData = jedis.hget(KEY, serverId);
            return rawData == null ? null : JsonUtil.fromJson(rawData, GameServer.class);
        });
    }

    @NotNull
    public List<GameServer> queryAllServersInNetwork() {
        return connector.supplyFromJedis(jedis -> {
            final var rawData = jedis.hvals(KEY);
            return rawData.stream()
              .map(data -> JsonUtil.fromJson(data, GameServer.class))
              .filter(Objects::nonNull)
              .toList();
        });
    }

    @NotNull
    public List<GameServer> queryServersByCategoryType(@NotNull ServerCategoryType categoryType) {
        final var categoryTypeName = categoryType.name();
        return connector.supplyFromJedis(jedis -> {
            final var serverIds = jedis.smembers(CATEGORY_IDX_KEY + categoryTypeName);
            if (serverIds.isEmpty()) {
                return Collections.emptyList();
            }

            final var rawData = jedis.hmget(KEY, serverIds.toArray(new String[0]));
            return rawData.stream()
              .map(data -> JsonUtil.fromJson(data, GameServer.class))
              .filter(Objects::nonNull)
              .toList();
        });
    }

    public void update(@NotNull GameServer server) {
        connector.useJedis(jedis -> {
            final var rawData = JsonUtil.toJson(server);
            final var categoryId = server.categoryType().name();

            jedis.hset(KEY, server.serverId(), rawData);
            jedis.sadd(CATEGORY_IDX_KEY + categoryId, server.serverId());
            log.info("Servidor {} atualizado na rede com sucesso.", server.serverId());
        });
    }

    public void unregister(@NotNull String serverId) {
        connector.useJedis(jedis -> {
            final var server = getById(serverId);
            if (server != null) {
                jedis.hdel(KEY, serverId);
                jedis.srem(CATEGORY_IDX_KEY + server.categoryType().name(), serverId);

                log.info("Servidor {} desregistrado da rede com sucesso.", serverId);
            }
        });
    }
}
