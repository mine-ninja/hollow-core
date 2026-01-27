package io.github.minehollow.sdk.player.wallet.log;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletBalanceLog(
    @JsonProperty("id") UUID logId,
    @JsonProperty("pid") UUID playerId,
    @JsonProperty("cid") String currencyId,
    @JsonProperty("amt") BigDecimal amount,
    @JsonProperty("type") WalletBalanceLogType type,
    @JsonProperty("reason") String reason,
    @JsonProperty("ts") long timestamp
) {

    public static WalletBalanceLog createLog(UUID playerId, String currencyId, BigDecimal amount, WalletBalanceLogType type, String reason) {
        return new WalletBalanceLog(UUID.randomUUID(), playerId, currencyId, amount, type, reason, Instant.now().toEpochMilli());
    }
}
