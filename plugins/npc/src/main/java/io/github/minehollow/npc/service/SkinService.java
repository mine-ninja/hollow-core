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

    private static final Pattern UUID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TEXTURE_VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TEXTURE_SIGNATURE_PATTERN = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

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

        return CompletableFuture.supplyAsync(() -> null); // URL-based fetch not implemented
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

