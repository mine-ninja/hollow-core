package io.github.minehollow.minecraft.wallet;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.event.wallet.PlayerWalletBalanceChangeEvent;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class PlayerWalletBukkitService {

    private final BukkitPlatform platform;


    public BigDecimal getPlayerBalance(@NotNull UUID playerId, @NotNull String currencyId) {
        final var wallet = platform.getWalletService().getCachedWallet(playerId);
        if (wallet == null) {
            return BigDecimal.ZERO;
        }

        return wallet.getCurrencyAmount(currencyId);
    }

    public boolean hasSufficientBalance(
      @NotNull UUID playerId,
      @NotNull String currencyId,
      @NotNull BigDecimal amount
    ) {
        final var wallet = platform.getWalletService().getCachedWallet(playerId);
        if (wallet == null) {
            return false;
        }

        return wallet.getCurrencyAmount(currencyId).compareTo(amount) >= 0;
    }

    public void setCurrencyValue(
      @NotNull UUID playerId,
      @NotNull String currencyId,
      @NotNull BigDecimal newAmount
    ) {
        final var wallet = platform.getWalletService().getCachedWallet(playerId);
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found for player: " + playerId);
        }

        final var actualBalance = wallet.getCurrencyAmount(currencyId);

        final var event = new PlayerWalletBalanceChangeEvent(playerId, currencyId, actualBalance, newAmount);
        if (!event.callEvent()) {
            return;
        }

        wallet.setCurrencyAmount(currencyId, event.getNewBalance());
        platform.getExecutorService().execute(() -> platform.getWalletService().updateWallet(wallet));
    }

    public void addCurrencyValue(
      @NotNull UUID playerId,
      @NotNull String currencyId,
      @NotNull BigDecimal amountToAdd
    ) {
        final var wallet = platform.getWalletService().getCachedWallet(playerId);
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found for player: " + playerId);
        }

        final var actualBalance = wallet.getCurrencyAmount(currencyId);
        final var newBalance = actualBalance.add(amountToAdd);

        final var event = new PlayerWalletBalanceChangeEvent(playerId, currencyId, actualBalance, newBalance);
        if (!event.callEvent()) {
            return;
        }

        wallet.setCurrencyAmount(currencyId, event.getNewBalance());
        platform.getExecutorService().execute(() -> platform.getWalletService().updateWallet(wallet));
    }


    public void subtractCurrencyValue(
      @NotNull UUID playerId,
      @NotNull String currencyId,
      @NotNull BigDecimal amountToSubtract
    ) {
        final var wallet = platform.getWalletService().getCachedWallet(playerId);
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found for player: " + playerId);
        }

        final var actualBalance = wallet.getCurrencyAmount(currencyId);
        var newBalance = actualBalance.subtract(amountToSubtract);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }

        final var event = new PlayerWalletBalanceChangeEvent(playerId, currencyId, actualBalance, newBalance);
        if (!event.callEvent()) {
            return;
        }

        wallet.setCurrencyAmount(currencyId, event.getNewBalance());
        platform.getExecutorService().execute(() -> platform.getWalletService().updateWallet(wallet));
    }
}
