package io.github.minehollow.clans.service;

import io.github.minehollow.clans.config.MessageConfig;
import io.github.minehollow.minecraft.util.message.StringUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Result codes for clan operations. Messages are loaded from messages.yml via {@link MessageConfig}.
 */
@Getter
@RequiredArgsConstructor
public enum ClanResult {

    SUCCESS("result.success"),
    ALREADY_IN_CLAN("result.already-in-clan"),
    NOT_IN_CLAN("result.not-in-clan"),
    TAG_TAKEN("result.tag-taken"),
    NAME_TAKEN("result.name-taken"),
    CLAN_NOT_FOUND("result.clan-not-found"),
    NOT_OWNER("result.not-owner"),
    NO_PERMISSION("result.no-permission"),
    OWNER_CANNOT_LEAVE("result.owner-cannot-leave"),
    CANNOT_KICK_OWNER("result.cannot-kick-owner"),
    TARGET_IN_CLAN("result.target-in-clan"),
    TARGET_NOT_IN_CLAN("result.target-not-in-clan"),
    NOT_INVITED("result.not-invited"),
    ALREADY_INVITED("result.already-invited"),
    CLAN_FULL("result.clan-full"),
    MAX_TIER("result.max-tier"),
    INSUFFICIENT_FUNDS("result.insufficient-funds"),
    INVALID_TAG("result.invalid-tag"),
    INVALID_NAME("result.invalid-name");

    private final String messageKey;

    /**
     * Returns the resolved message from messages.yml.
     */
    public @NotNull Component getMessage() {
        MessageConfig cfg = MessageConfig.getInstance();
        var msg = (cfg == null) ? "§c" + messageKey : cfg.get(messageKey);
        return StringUtils.formatString(msg);
    }
}

