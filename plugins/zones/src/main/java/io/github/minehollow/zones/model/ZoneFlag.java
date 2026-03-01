package io.github.minehollow.zones.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum ZoneFlag {
    BLOCK_BREAK("block-break"),
    BLOCK_PLACE("block-place"),
    BLOCK_PHYSICS("block-physics"),
    BLOCK_TICK("block-tick"),
    ENTITY_SPAWN("entity-spawn"),
    MOB_SPAWN("mob-spawn"),
    ITEM_DROP("item-drop"),
    ITEM_PICKUP("item-pickup"),
    PVP("pvp"),
    HIDE_PLAYERS("hide-players"),
    COMMAND_EXECUTE("command-execute"),
    INTERACT("interact");

    private final String configKey;

    ZoneFlag(@NotNull String configKey) {
        this.configKey = configKey;
    }

    @NotNull
    public String configKey() {
        return configKey;
    }

    @Nullable
    public static ZoneFlag fromKey(@NotNull String key) {
        for (ZoneFlag flag : values()) {
            if (flag.configKey.equals(key.toLowerCase(Locale.ROOT))) return flag;
        }
        return null;
    }
}

