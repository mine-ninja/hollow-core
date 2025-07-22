package net.warcane.lugin.core.network.channel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public enum NetworkChannel {

    PLAYER_CONNECTION,
    PLAYER_MESSAGE,
    SERVER_STATUS,
    FRIENDS,
    PARTY,
    MATCHMAKING,
    OPERATION;

    public static final Map<String, NetworkChannel> entries = Arrays.stream(NetworkChannel.values())
      .collect(toMap(NetworkChannel::name, Function.identity()));


    public static @Nullable NetworkChannel fromString(@NotNull String name) {
        return entries.get(name.toUpperCase());
    }
}
