package io.github.minehollow.minecraft.nametag.resolver;

import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.sdk.player.account.PlayerAccount;

public class LegacyNameTagResolver extends NameTagResolver {
    public LegacyNameTagResolver(BukkitPlatform platform) {
        this.tagPrefix = (player ->  {
            PlayerAccount account = platform.getPlayerAccountService().getCachedAccount(player.getUniqueId());
            return account.getHighestSubscription().group().getPrefix();
        });
        this.tagColor = (player -> {
            PlayerAccount account = platform.getPlayerAccountService().getCachedAccount(player.getUniqueId());
            return "&" + account.getHighestSubscription().group().getPrefixColorCode();
        });
    }
}
