package io.github.minehollow.minecraft.wallet;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.minecraft.event.wallet.PlayerWalletBalanceChangeEvent;
import io.github.minehollow.minecraft.event.wallet.PlayerWalletBalanceChangeEvent.ChangeType;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.sdk.player.wallet.Wallet;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
        @NotNull BigDecimal newAmount,
        @NotNull WalletTransactionContext context
    ) {
        final var wallet = platform.getWalletService().getOrLoadWallet(playerId);
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found for player: " + playerId);
        }

        final var actualBalance = wallet.getCurrencyAmount(currencyId);
        fireAndApply(wallet, playerId, currencyId, actualBalance, newAmount, ChangeType.SET, context);
    }

    public void addCurrencyValue(
        @NotNull UUID playerId,
        @NotNull String currencyId,
        @NotNull BigDecimal amountToAdd,
        @NotNull WalletTransactionContext context
    ) {
        final var wallet = platform.getWalletService().getOrLoadWallet(playerId);
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found for player: " + playerId);
        }

        final var actualBalance = wallet.getCurrencyAmount(currencyId);
        final var newBalance = actualBalance.add(amountToAdd);
        fireAndApply(wallet, playerId, currencyId, actualBalance, newBalance, ChangeType.ADD, context);
    }

    public void subtractCurrencyValue(
        @NotNull UUID playerId,
        @NotNull String currencyId,
        @NotNull BigDecimal amountToSubtract,
        @NotNull WalletTransactionContext context
    ) {
        final var wallet = platform.getWalletService().getOrLoadWallet(playerId);
        if (wallet == null) {
            throw new IllegalStateException("Wallet not found for player: " + playerId);
        }

        final var actualBalance = wallet.getCurrencyAmount(currencyId);
        final var newBalance = actualBalance.subtract(amountToSubtract).max(BigDecimal.ZERO);
        fireAndApply(wallet, playerId, currencyId, actualBalance, newBalance, ChangeType.SUBTRACT, context);
    }

    // ---

    private void fireAndApply(
        @NotNull Wallet wallet,
        @NotNull UUID playerId,
        @NotNull String currencyId,
        @NotNull BigDecimal oldBalance,
        @NotNull BigDecimal newBalance,
        @NotNull ChangeType changeType,
        @NotNull WalletTransactionContext context
    ) {
        final var event = new PlayerWalletBalanceChangeEvent(
            playerId, currencyId, oldBalance, newBalance, changeType, context
        );

        if (!event.callEvent()) {
            return;
        }

        wallet.setCurrencyAmount(currencyId, event.getNewBalance());
        Tasks.runAsync(() -> platform.getWalletService().updateWallet(wallet));
    }
}