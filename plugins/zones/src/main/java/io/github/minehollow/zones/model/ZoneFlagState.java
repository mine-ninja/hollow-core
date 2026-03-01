package io.github.minehollow.zones.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum ZoneFlagState {
    ALLOW,
    DENY,
    NONE;

    @Nullable
    public static ZoneFlagState fromString(@NotNull String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "allow" -> ALLOW;
            case "deny" -> DENY;
            case "none" -> NONE;
            default -> null;
        };
    }
}

