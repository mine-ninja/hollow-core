package net.warcane.lugin.core.minecraft.skin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Classe responsável por buscar informações de skins de jogadores de Minecraft a partir de APIs externas.
 * Utiliza endpoints para obter dados de skins com base no UUID do jogador e armazena resultados em cache.
 */
public class SkinFetcher {

    /**
     * Instância singleton do SkinFetcher.
     */
    public static final SkinFetcher INSTANCE = new SkinFetcher();

    /**
     * Parser JSON para processar respostas das APIs.
     */
    private static final JsonParser JSON_PARSER = new JsonParser();


    /**
     * Endpoint do playerdb.co para buscar informações de jogadores.
     */
    private static final String PLAYER_DB_ENDPOINT = "https://playerdb.co/api/player/minecraft/%s";


    /**
     * Endpoints das APIs para busca de skins.
     */
    private static final String[] UUID_ENDPOINTS = new String[]{
      "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false",
      "https://api.mineskin.org/get/uuid/%s"
    };

    /**
     * Temporary cache of skins by player name.
     */
    private static final Cache<String, Skin> CACHED_BY_NAME = CacheBuilder.newBuilder()
      .maximumSize(500)
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .build();

    /**
     * Temporary cache of skins by player UUID.
     */
    private static final Cache<UUID, Skin> CACHED_BY_UUID = CacheBuilder.newBuilder()
      .maximumSize(500)
      .expireAfterWrite(10, TimeUnit.MINUTES)
      .build();


    /**
     * Realiza uma requisição para buscar a skin de um jogador com base no UUID.
     *
     * @param uuid UUID do jogador cuja skin será buscada.
     * @return Objeto Skin com os dados da skin ou null se a requisição falhar.
     */
    public Skin fetchByUniqueId(@NotNull UUID uuid) {
        return fetchByUniqueId(0, UUID_ENDPOINTS[0], uuid);
    }

    /**
     * Busca a skin de um jogador pelo nome usando a API da Ashcon.
     *
     * @param username Nome do jogador cuja skin será buscada.
     * @return Objeto Skin com os dados da skin ou null se a requisição falhar.
     */
    public Skin fetchByUsername(@NotNull String username) {
        Skin cachedSkin = CACHED_BY_NAME.getIfPresent(username);
        if (cachedSkin != null) {
            return cachedSkin;
        }

        try {
            JsonElement element = fetchJsonFromApi(PLAYER_DB_ENDPOINT, username);
            if (element instanceof JsonObject object) {
                Skin skin = processPlayerDBResponse(object);
                if (skin != null) {
                    CACHED_BY_NAME.put(username, skin);
                    CACHED_BY_UUID.put(UUID.fromString(skin.getUuid()), skin);
                    return skin;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao buscar skin pelo nome: " + username, e);
        }
        return null;
    }

    /**
     * Processa a resposta JSON da API da Ashcon e cria um objeto Skin.
     *
     * @param object Objeto JSON da resposta.
     * @return Objeto Skin com os dados da skin ou null se não houver dados válidos.
     */
    private Skin processAshconResponse(JsonObject object) {
        if (object.has("uuid") && object.has("username") && object.has("textures")) {
            String uuid = object.get("uuid").getAsString();
            String username = object.get("username").getAsString();
            JsonObject textures = object.getAsJsonObject("textures");
            JsonObject raw = textures.getAsJsonObject("raw");

            if (raw != null && raw.has("value") && raw.has("signature")) {
                Skin skin = new Skin(
                  raw.get("value").getAsString(),
                  raw.get("signature").getAsString()
                );
                skin.setUuid(uuid);
                skin.setName(username);
                return skin;
            }
        }
        return null;
    }


    /**
     * Processa a resposta JSON da API da PlayerDB e cria um objeto Skin.
     *
     * @param object Objeto JSON da resposta.
     * @return Objeto Skin com os dados da skin ou null se não houver dados válidos.
     */
    private Skin processPlayerDBResponse(JsonObject object) {
        if (!object.get("success").getAsBoolean()) return null;

        JsonObject player = object.getAsJsonObject("data").getAsJsonObject("player");
        if (player == null || !player.has("id") || !player.has("username") || !player.has("properties")) {
            return null;
        }

        String uuid = player.get("id").getAsString();
        String username = player.get("username").getAsString();
        JsonArray properties = player.getAsJsonArray("properties");

        for (JsonElement prop : properties) {
            JsonObject property = prop.getAsJsonObject();
            if ("textures".equals(property.get("name").getAsString())
                && property.has("value") && property.has("signature")) {
                Skin skin = new Skin(property.get("value").getAsString(), property.get("signature").getAsString());
                skin.setUuid(uuid);
                skin.setName(username);
                return skin;
            }
        }
        return null;
    }

    /**
     * Realiza uma requisição para buscar a skin de um jogador com base no UUID.
     * Tenta usar as APIs disponíveis em sequência até obter uma resposta válida ou esgotar as opções.
     *
     * @param idx  Índice do endpoint atual na lista de ENDPOINTS.
     * @param api  URL da API a ser utilizada.
     * @param uuid UUID do jogador cuja skin será buscada.
     * @return Objeto Skin com os dados da skin ou null se a requisição falhar.
     */
    private Skin fetchByUniqueId(int idx, String api, UUID uuid) {
        Skin cachedSkin = CACHED_BY_UUID.getIfPresent(uuid);
        if (cachedSkin != null) {
            return cachedSkin;
        }

        try {
            JsonElement element = fetchJsonFromApi(api, uuid);
            if (element instanceof JsonObject object) {
                if (hasError(object)) {
                    throw new Exception(object.get("errorMessage").getAsString());
                }
                Skin fromResponse = processJsonResponse(object, uuid);
                if (fromResponse != null) {
                    CACHED_BY_UUID.put(uuid, fromResponse);
                    CACHED_BY_NAME.put(fromResponse.getName(), fromResponse);
                    return fromResponse;
                }
            }
        } catch (Exception e) {
            System.out.println("Falha na api " + api);
            return tryNextEndpoint(idx, uuid);
        }
        return null;
    }


    /**
     * Faz a requisição HTTP para a API e retorna o elemento JSON da resposta.
     *
     * @param api      URL da API formatada com o nome de usuário.
     * @param username Nome de usuário do jogador.
     * @return JsonElement com a resposta da API.
     * @throws Exception Se a requisição falhar.
     */
    private JsonElement fetchJsonFromApi(String api, String username) throws Exception {
        URLConnection con = new URL(String.format(api, username)).openConnection();
        return JSON_PARSER.parse(new BufferedReader(
          new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)
        ));
    }

    /**
     * Faz a requisição HTTP para a API e retorna o elemento JSON da resposta.
     *
     * @param api  URL da API formatada com o UUID.
     * @param uuid UUID do jogador.
     * @return JsonElement com a resposta da API.
     * @throws Exception Se a requisição falhar.
     */
    private JsonElement fetchJsonFromApi(String api, UUID uuid) throws Exception {
        URLConnection con = new URL(String.format(api, uuid.toString())).openConnection();
        return JSON_PARSER.parse(new BufferedReader(
          new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))
        );
    }

    /**
     * Verifica se a resposta JSON contém um erro.
     *
     * @param object Objeto JSON da resposta.
     * @return true se houver erro, false caso contrário.
     */
    private boolean hasError(JsonObject object) {
        return object.has("error") && object.has("errorMessage");
    }

    /**
     * Processa a resposta JSON e constrói o objeto Skin com base nos dados recebidos.
     *
     * @param object Objeto JSON da resposta.
     * @param uuid   UUID do jogador.
     * @return Objeto Skin com os dados extraídos ou null se não houver dados válidos.
     */
    private Skin processJsonResponse(JsonObject object, UUID uuid) {
        if (object.has("properties")) {
            return processMojangResponse(object, uuid);
        } else if (object.has("data")) {
            return processMineskinResponse(object);
        }
        return null;
    }

    /**
     * Processa a resposta da API da Mojang e cria um objeto Skin.
     *
     * @param object Objeto JSON da resposta.
     * @param uuid   UUID do jogador.
     * @return Objeto Skin com os dados da skin.
     */
    private Skin processMojangResponse(JsonObject object, UUID uuid) {
        JsonArray properties = object.getAsJsonArray("properties");
        if (properties.size() > 0) {
            JsonObject jsonProperty = properties.get(0).getAsJsonObject();
            String property = jsonProperty.toString();

            Skin stoneSkin = new Skin();
            stoneSkin.setUuid(uuid.toString());
            stoneSkin.setPropertiesSkin(property);
            stoneSkin.setName(object.get("name").getAsString());
            return stoneSkin;
        }
        return null;
    }

    /**
     * Processa a resposta da API Mineskin e cria um objeto Skin.
     *
     * @param object Objeto JSON da resposta.
     * @return Objeto Skin com os dados da skin.
     */
    private Skin processMineskinResponse(JsonObject object) {
        JsonObject texture = object.get("data").getAsJsonObject().get("texture").getAsJsonObject();
        return new Skin(
          texture.get("value").getAsString(),
          texture.get("signature").getAsString()
        );
    }

    /**
     * Tenta o próximo endpoint disponível caso a requisição atual falhe.
     *
     * @param idx  Índice do endpoint atual.
     * @param uuid UUID do jogador.
     * @return Objeto Skin da próxima API ou null se não houver mais endpoints.
     */
    private Skin tryNextEndpoint(int idx, UUID uuid) {
        idx++;
        if (idx < UUID_ENDPOINTS.length) {
            String nextApi = UUID_ENDPOINTS[idx];
            return fetchByUniqueId(idx, nextApi, uuid);
        }
        return null;
    }
}