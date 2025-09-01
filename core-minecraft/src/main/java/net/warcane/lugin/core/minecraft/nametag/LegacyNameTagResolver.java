package net.warcane.lugin.core.minecraft.nametag;

import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.player.account.PlayerAccount;

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
