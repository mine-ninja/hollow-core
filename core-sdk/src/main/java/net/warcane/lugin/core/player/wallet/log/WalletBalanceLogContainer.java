package net.warcane.lugin.core.player.wallet.log;

import org.bson.codecs.pojo.annotations.BsonId;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record WalletBalanceLogContainer(
  @NotNull @BsonId UUID playerId,
  @NotNull List<WalletBalanceLog> logs
) {

    public static WalletBalanceLogContainer createEmpty(@NotNull UUID playerId) {
        return new WalletBalanceLogContainer(playerId, new ArrayList<>());
    }

    public void addLog(@NotNull WalletBalanceLog log) {
        logs.add(log);
    }
}