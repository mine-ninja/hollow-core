package io.github.minehollow.sdk.player.wallet.transaction;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public sealed interface TransactionResult {

    static TransactionResult success(UUID sourceWalletId, UUID targetWalletId, String currencyId, BigDecimal amount) {
        return new Success(sourceWalletId, targetWalletId, currencyId, amount);
    }

    static TransactionResult insufficientFunds(String currencyId, BigDecimal requiredAmount, BigDecimal providedAmount) {
        return new InsufficientFunds(currencyId, requiredAmount, providedAmount);
    }

    static TransactionResult invalidCurrency(String requiredCurrency, String providedCurrency) {
        return new InvalidCurrency(requiredCurrency, providedCurrency);
    }

    static TransactionResult unknownError(String message) {
        return new UnknownError(message);
    }

    static TransactionResult selfTransferError() {
        return new UnknownError("Você não pode transferir dinheiro para a sua própria carteira.");
    }

    static TransactionResult walletNotFound(UUID walletId) {
        return new WalletNotFound(walletId);
    }

    static TransactionResult invalidAmount(@NotNull BigDecimal amount) {
        return new InvalidAmount(amount);
    }

    static TransactionResult failure(Throwable e) {
        return new Failure(e);
    }

    record Success(UUID sourceWalletId, UUID targetWalletId, String currencyId,
                   BigDecimal amount) implements TransactionResult {
    }

    record InsufficientFunds(String currencyId, BigDecimal requiredAmount, BigDecimal providedAmount)
      implements TransactionResult {
        public BigDecimal difference() {
            return requiredAmount.subtract(providedAmount);
        }
    }

    record InvalidCurrency(String requiredCurrency, String providedCurrency) implements TransactionResult {
    }

    record UnknownError(String message) implements TransactionResult {
    }

    record InvalidAmount(@NotNull BigDecimal providedAmount) implements TransactionResult {
    }

    record WalletNotFound(UUID walletId) implements TransactionResult {
    }

    record Failure(Throwable throwable) implements TransactionResult {

    }
}
