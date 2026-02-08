package io.github.minehollow.lobby.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.tofaa.entitylib.extras.skin.SkinFetcher;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkinService {

    private static final String MINESKIN_API_URL = "https://api.mineskin.org/v2/queue";
    private static final String MINESKIN_API_KEY = "msk_mHGQV2ln_AhOAsAIAmJuChV4G4cXnVo7oT6A5ywfNv5oiVCfKYqziP9QpzSGiFTPMMJNPPh-F";

    private static final Pattern SKINSMC_PATTERN = Pattern.compile("skinsmc\\.org/skin/(\\d+)");
    private static final Pattern TEXTURE_VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TEXTURE_SIGNATURE_PATTERN = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

    private final SkinFetcher mojangFetcher = SkinFetcher.builder().build();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final Cache<String, SkinData> skinCache = Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(30))
      .maximumSize(100)
      .build();

    private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

    public CompletableFuture<SkinData> fetchSkinByName(@NotNull String playerName) {
        SkinData cached = skinCache.getIfPresent(playerName.toLowerCase());
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return CompletableFuture.supplyAsync(() -> {
            try {
                var props = mojangFetcher.getSkin(playerName);
                if (props == null || props.isEmpty()) return null;

                var data = new SkinData(props.get(0).getValue(), props.get(0).getSignature());
                skinCache.put(playerName.toLowerCase(), data);
                return data;
            } catch (Exception e) {
                return null;
            }
        }, executor);
    }

    public CompletableFuture<SkinData> fetchSkinByURL(@NotNull String url) {
        String finalUrl = normalizeUrl(url);
        SkinData cached = skinCache.getIfPresent(finalUrl);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchFromMineSkin(finalUrl);
            } catch (Exception e) {
                return null;
            }
        }, executor);
    }

    private String normalizeUrl(String url) {
        Matcher matcher = SKINSMC_PATTERN.matcher(url);
        return matcher.find() ? "https://skinsmc.org/download/" + matcher.group(1) : url;
    }

    private SkinData fetchFromMineSkin(String skinUrl) throws Exception {
        String jsonBody = "{\"url\":\"" + skinUrl + "\",\"variant\":\"classic\",\"visibility\":\"unlisted\"}";
        HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(MINESKIN_API_URL))
          .header("Authorization", "Bearer " + MINESKIN_API_KEY)
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
          .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return (response.statusCode() == 200) ? parseSkinData(response.body()) : null;
    }

    private SkinData parseSkinData(String json) {
        Matcher v = TEXTURE_VALUE_PATTERN.matcher(json);
        Matcher s = TEXTURE_SIGNATURE_PATTERN.matcher(json);
        return (v.find() && s.find()) ? new SkinData(v.group(1), s.group(1)) : null;
    }

    public record SkinData(@NotNull String texture, @NotNull String signature) {
    }
}