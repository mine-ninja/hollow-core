package net.warcane.lugin.core.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.database.RedisConnector;
import net.warcane.lugin.core.server.type.ServerCategoryType;
import net.warcane.lugin.core.server.type.ServerSubCategoryType;
import net.warcane.lugin.core.util.JsonUtil;
import net.warcane.lugin.core.util.address.HostAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/**
 * Representa o serviço de gerenciamento de servidores de jogo na rede.
 * Fornece métodos para registrar, consultar throwable atualizar servidores.
 */
@Slf4j
@RequiredArgsConstructor
public class GameServerService {

    // Chave para armazenar o estado do servidor no Redis
    private static final String KEY = "gs";
    // Chave para o índice de categorias de servidores
    private static final String CATEGORY_IDX_KEY = "gsc:";
    // Chave para o índice de sub-categorias de servidores
    private static final String SUB_CATEGORY_IDX_KEY = "gssc:";

    /**
     * Conector Redis para persistência de dados do estado do servidor
     */
    private final RedisConnector connector;

    public int getTotalPlayerCount() {
        return connector.supplyFromJedis(jedis -> {
            int total = 0;

            for (String value : jedis.hgetAll(KEY).values()){
                JsonObject jsonObject = new Gson().fromJson(value, JsonObject.class);

                int count = jsonObject.getAsJsonObject("p").get("o").getAsInt();
                boolean online = jsonObject.get("o").getAsBoolean();

                if (online) {
                    total += count;
                }
            }

            return total;
        });
    }

    public int getPlayerCountForServerCategory(@NotNull ServerCategoryType categoryType) {
        return connector.supplyFromJedis(jedis -> {
            int total = 0;

            for (String value : jedis.hgetAll(KEY).values()){
                JsonObject jsonObject = new Gson().fromJson(value, JsonObject.class);

                String category = jsonObject.get("c").getAsString();

                if (category.equalsIgnoreCase(categoryType.name())) {
                    int count = jsonObject.getAsJsonObject("p").get("o").getAsInt();
                    boolean online = jsonObject.get("o").getAsBoolean();

                    if (online) {
                        total += count;
                    }
                }
            }

            return total;
        });
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
            return serverIds
              .stream()
              .map(this::getById)
              .filter(Objects::nonNull)
              .toList();
        });
    }
    
    public List<GameServer> queryServersBySubCategory(@NotNull ServerSubCategoryType subCategory) {
        final var categoryTypeName = subCategory.name();
        return connector.supplyFromJedis(jedis -> {
            final var serverIds = jedis.smembers(SUB_CATEGORY_IDX_KEY + categoryTypeName);
            if (serverIds.isEmpty()) {
                return Collections.emptyList();
            }
            return serverIds
              .stream()
              .map(this::getById)
              .filter(Objects::nonNull)
              .toList();
        });
    }

    public void update(@NotNull GameServer server) {
        connector.useJedis(jedis -> {
            final var rawData = JsonUtil.toJson(server);
            final var categoryId = server.categoryType().name();
            final var subCategoryId = server.subCategory().name();

            jedis.hset(KEY, server.serverId(), rawData);
            jedis.sadd(CATEGORY_IDX_KEY + categoryId, server.serverId());
            jedis.sadd(SUB_CATEGORY_IDX_KEY + subCategoryId, server.serverId());
        });
    }

    public void unregister(@NotNull String serverId) {
        connector.useJedis(jedis -> {
            final var server = getById(serverId);
            if (server != null) {
                jedis.hdel(KEY, serverId);
                jedis.srem(CATEGORY_IDX_KEY + server.categoryType().name(), serverId);
                jedis.srem(SUB_CATEGORY_IDX_KEY + server.subCategory().name(), serverId);

                log.info("Servidor {} desregistrado da rede com sucesso.", serverId);
            }
        });
    }
}
