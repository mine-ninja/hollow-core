package io.github.minehollow.minecraft.event.wallet;

import io.github.minehollow.minecraft.wallet.WalletTransactionContext;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class PlayerWalletBalanceChangeEvent extends Event implements Cancellable {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final UUID playerId;
    private final String currencyId;
    private final BigDecimal oldBalance;
    private BigDecimal newBalance;
    private final ChangeType changeType;
    private final WalletTransactionContext context;

    private boolean cancelled = false;

    public PlayerWalletBalanceChangeEvent(
        @NotNull UUID playerId,
        @NotNull String currencyId,
        @NotNull BigDecimal oldBalance,
        @NotNull BigDecimal newBalance,
        @NotNull ChangeType changeType,
        @NotNull WalletTransactionContext context
    ) {
        super(!Bukkit.isPrimaryThread());
        this.playerId = playerId;
        this.currencyId = currencyId;
        this.oldBalance = oldBalance;
        this.newBalance = newBalance;
        this.changeType = changeType;
        this.context = context;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    public boolean isIncrease() {
        return newBalance.compareTo(oldBalance) > 0;
    }

    public boolean isDecrease() {
        return newBalance.compareTo(oldBalance) < 0;
    }

    public BigDecimal getDelta() {
        return newBalance.subtract(oldBalance);
    }

    public enum ChangeType {
        SET,
        ADD,
        SUBTRACT
    }
}