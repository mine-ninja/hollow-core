package net.warcane.lugin.core.player.wallet.log;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletBalanceLog(
  @JsonProperty("id") UUID logId,
  @JsonProperty("pid") UUID playerId,
  @JsonProperty("cid") String currencyId,
  @JsonProperty("amt") BigDecimal amount,
  @JsonProperty("type") WalletBalanceLogType type,
  @JsonProperty("ts") long timestamp
) {

    public static WalletBalanceLog createLog(
      UUID playerId,
      String currencyId,
      BigDecimal amount,
      WalletBalanceLogType type
    ) {
        return new WalletBalanceLog(
          UUID.randomUUID(),
          playerId,
          currencyId,
          amount,
          type,
          System.currentTimeMillis()
        );
    }


    public enum WalletBalanceLogType {
        ADDITION,
        SUBTRACTION,
        UPDATE
    }
}
