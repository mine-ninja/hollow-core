package io.github.minehollow.npc.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.tofaa.entitylib.extras.skin.SkinFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches Minecraft skins asynchronously.
 * <ul>
 *     <li>{@link #fetchByName(String)} — Mojang profile lookup (online or offline player names).</li>
 *     <li>{@link #fetchByUrl(String)} — Upload a skin URL to MineSkin and get texture data.</li>
 * </ul>
 * Results are cached for 30 minutes.
 */
public class SkinService {

    private static final String MOJANG_PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String MOJANG_SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String MINESKIN_GET_BY_UUID_URL = "https://api.mineskin.org/get/uuid/";
    private static final String MINESKIN_GET_BY_ID_URL = "https://api.mineskin.org/get/id/";

    private static final Pattern UUID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TEXTURE_VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TEXTURE_SIGNATURE_PATTERN = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern MINESKIN_UUID_TOKEN = Pattern.compile("(?i)^[0-9a-f]{32}$|^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    private static final Pattern MINESKIN_ID_TOKEN = Pattern.compile("^[0-9]{1,10}$");

    private final Cache<String, SkinData> cache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(30))
        .maximumSize(200)
        .build();

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    /**
     * Fetches skin data for a player name via EntityLib's {@link SkinFetcher} (Mojang API).
     * Falls back to a direct HTTP Mojang lookup if SkinFetcher fails.
     * The returned future may contain {@code null} if the skin could not be found.
     */
    public @NotNull CompletableFuture<SkinData> fetchByName(@NotNull String playerName) {
        String key = playerName.toLowerCase();
        SkinData cached = cache.getIfPresent(key);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return CompletableFuture.supplyAsync(() -> {
            // Try EntityLib SkinFetcher first
            try {
                SkinFetcher fetcher = SkinFetcher.builder().build();
                var props = fetcher.getSkin(playerName);
                if (props != null && !props.isEmpty()) {
                    var first = props.getFirst();
                    String sig = first.getSignature() != null ? first.getSignature() : "";
                    SkinData data = new SkinData(first.getValue(), sig);
                    cache.put(key, data);
                    return data;
                }
            } catch (Exception ignored) {}

            // Fallback: direct Mojang API
            try {
                return fetchFromMojang(playerName);
            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Fetches skin data by texture URL via a direct Mojang-style session lookup.
     * The returned future may contain {@code null} if the skin could not be found.
     */
    public @NotNull CompletableFuture<SkinData> fetchByUrl(@NotNull String url) {
        SkinData cached = cache.getIfPresent(url);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String token = extractMineSkinToken(url);
                if (token == null || token.isBlank()) return null;

                SkinData data = fetchFromMineSkin(token);
                if (data != null) {
                    cache.put(url, data);
                    cache.put(token.toLowerCase(), data);
                }
                return data;
            } catch (Exception e) {
                return null;
            }
        });
    }

    private @Nullable String extractMineSkinToken(@NotNull String url) {
        String raw = url.trim();
        if (MINESKIN_UUID_TOKEN.matcher(raw).matches() || MINESKIN_ID_TOKEN.matcher(raw).matches()) {
            return raw;
        }

        try {
            URI uri = URI.create(raw);

            String query = uri.getQuery();
            if (query != null && !query.isBlank()) {
                for (String part : query.split("&")) {
                    String[] kv = part.split("=", 2);
                    if (kv.length == 2) {
                        String value = kv[1];
                        if (MINESKIN_UUID_TOKEN.matcher(value).matches() || MINESKIN_ID_TOKEN.matcher(value).matches()) {
                            return value;
                        }
                    }
                }
            }

            String path = uri.getPath();
            if (path == null || path.isBlank()) return null;
            String[] segments = path.split("/");
            for (int i = segments.length - 1; i >= 0; i--) {
                String seg = segments[i];
                if (seg == null || seg.isBlank()) continue;
                if (seg.equalsIgnoreCase("skin") || seg.equalsIgnoreCase("id") || seg.equalsIgnoreCase("uuid")) continue;
                if (MINESKIN_UUID_TOKEN.matcher(seg).matches() || MINESKIN_ID_TOKEN.matcher(seg).matches()) {
                    return seg;
                }
            }

            for (int i = segments.length - 1; i >= 0; i--) {
                String seg = segments[i];
                if (!seg.isBlank() && !seg.equalsIgnoreCase("skin")) {
                    return seg;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private @Nullable SkinData fetchFromMineSkin(@NotNull String token) throws Exception {
        SkinData byUuid = fetchFromMineSkinEndpoint(MINESKIN_GET_BY_UUID_URL + token);
        if (byUuid != null) return byUuid;
        return fetchFromMineSkinEndpoint(MINESKIN_GET_BY_ID_URL + token);
    }

    private @Nullable SkinData fetchFromMineSkinEndpoint(@NotNull String endpoint) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return null;

        String body = response.body();
        Matcher valueMatcher = TEXTURE_VALUE_PATTERN.matcher(body);
        Matcher sigMatcher = TEXTURE_SIGNATURE_PATTERN.matcher(body);

        if (valueMatcher.find() && sigMatcher.find()) {
            return new SkinData(valueMatcher.group(1), sigMatcher.group(1));
        }
        return null;
    }

    /**
     * Direct Mojang API lookup: username → UUID → session profile with textures.
     */
    private @Nullable SkinData fetchFromMojang(@NotNull String playerName) throws Exception {
        // Step 1: Get UUID from username
        HttpRequest uuidRequest = HttpRequest.newBuilder()
            .uri(URI.create(MOJANG_PROFILE_URL + playerName))
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> uuidResponse = httpClient.send(uuidRequest, HttpResponse.BodyHandlers.ofString());
        if (uuidResponse.statusCode() != 200) return null;

        Matcher uuidMatcher = UUID_PATTERN.matcher(uuidResponse.body());
        if (!uuidMatcher.find()) return null;
        String uuid = uuidMatcher.group(1);

        // Step 2: Get session profile with textures
        HttpRequest sessionRequest = HttpRequest.newBuilder()
            .uri(URI.create(MOJANG_SESSION_URL + uuid + "?unsigned=false"))
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

        HttpResponse<String> sessionResponse = httpClient.send(sessionRequest, HttpResponse.BodyHandlers.ofString());
        if (sessionResponse.statusCode() != 200) return null;

        String body = sessionResponse.body();
        Matcher valueMatcher = TEXTURE_VALUE_PATTERN.matcher(body);
        Matcher sigMatcher = TEXTURE_SIGNATURE_PATTERN.matcher(body);

        if (valueMatcher.find() && sigMatcher.find()) {
            SkinData data = new SkinData(valueMatcher.group(1), sigMatcher.group(1));
            cache.put(playerName.toLowerCase(), data);
            return data;
        }
        return null;
    }

    /**
     * Skin texture data (value + signature).
     */
    public record SkinData(@NotNull String value, @NotNull String signature) {}
}
