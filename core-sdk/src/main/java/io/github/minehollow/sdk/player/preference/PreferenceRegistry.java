package io.github.minehollow.sdk.player.preference;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PreferenceRegistry {

    private static final Map<String, PreferenceMetadata> metadataMap = new HashMap<>();

    public static final String LOBBY_PROTECTION_ID = "lobby_protection";
    public static final String PRIVATE_MESSAGES_ID = "private_messages";

    {
        register(LOBBY_PROTECTION_ID,
            new PreferenceMetadata(
                LOBBY_PROTECTION_ID,
                "Proteção do /lobby",
                List.of(
                    "<gray>Tenha que executar o comando /lobby",
                    "<gray>2 vezes para ir ao lobby"),
                true,
                (playerAccount, aBoolean) -> {}
            ));

        register(PRIVATE_MESSAGES_ID,
            new PreferenceMetadata(
                PRIVATE_MESSAGES_ID,
                "Mensagens Privadas",
                List.of(
                    "<gray>Permita que outros jogadores enviem",
                    "<gray>mensagens privadas para você"),
                true,
                (playerAccount, aBoolean) -> {}
            ));
    }

    public static void register(String id, PreferenceMetadata metadata) {
        metadataMap.put(id, metadata);
    }

    @NotNull
    public static Optional<PreferenceMetadata> get(String id) {
        return Optional.ofNullable(metadataMap.get(id));
    }

    @Contract(pure = true)
    public static @NotNull Collection<PreferenceMetadata> getAll() {
        return metadataMap.values();
    }
}
