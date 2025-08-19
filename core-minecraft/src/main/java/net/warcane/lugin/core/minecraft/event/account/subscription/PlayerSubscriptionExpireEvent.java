package net.warcane.lugin.core.minecraft.event.account.subscription;

import lombok.Getter;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.player.subscription.PlayerGroupSubscription;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerSubscriptionExpireEvent extends Event {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final PlayerAccount account;
    private final PlayerGroupSubscription expiredSubscription;

    public PlayerSubscriptionExpireEvent(PlayerAccount account, PlayerGroupSubscription expiredSubscription) {
        super(!Bukkit.isPrimaryThread());

        this.account = account;
        this.expiredSubscription = expiredSubscription;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }

    @NotNull
    public PlayerAccount getAccount() {
        return account;
    }

    @NotNull
    public PlayerGroupSubscription getExpiredSubscription() {
        return expiredSubscription;
    }
}
