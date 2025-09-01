package net.warcane.lugin.core.minecraft.nametag;

import net.warcane.lugin.core.player.account.PlayerAccount;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

public class LegacyNameTagResolver extends NameTagResolver {
    @Override
    public void applyNameTag(@NotNull PlayerAccount account) {
        final var localPlayer = this.getPlayer(account);
        if (localPlayer == null) return;
        
        final var group = account.getHighestSubscription().group();
        setNameTag(localPlayer, group.getPrefix(), "", "&" + group.getPrefixColorCode());
        updateAllTags();
    }
    
    @Override
    public void updateAllTags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            NameTag team = PLAYER_TAGS.get(player.getName());
            if (team != null) {
                setNameTag(player, team.prefix(), team.suffix(), team.color());
            }
        }
    }
}
