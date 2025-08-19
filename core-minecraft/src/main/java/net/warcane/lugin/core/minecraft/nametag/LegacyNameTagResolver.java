package net.warcane.lugin.core.minecraft.nametag;

import lombok.RequiredArgsConstructor;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.util.nametag.NameTags;
import net.warcane.lugin.core.player.account.PlayerAccount;
import net.warcane.lugin.core.util.property.Property;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class LegacyNameTagResolver implements NameTagResolver {

    private final BukkitPlatform platform;

    @Override
    public void applyNameTag(@NotNull PlayerAccount account) {
        final var localPlayer = this.getPlayer(account);
        if (localPlayer == null) return;

        final var currentSubscriptionType = platform.getSubscriptionCategoryType();
        final var highestSubscription = account.getHighestSubscription(currentSubscriptionType);
        final var group = highestSubscription.group();
        final var groupPrefix = group.getPrefix();
        final var priority = group.getPriorityValue();
        final var groupColor = group.getNamedTextColor();

        final var loadTagsOnJoin = Property.getBoolean("LOAD_TAGS_ON_JOIN", true);
        if (loadTagsOnJoin) {
            NameTags.setNameTag(localPlayer, groupPrefix, "", priority, groupColor);
            NameTags.updateAllTags();
        }
    }

    @Override
    public void removeNameTag(@NotNull PlayerAccount account) {
        final var localPlayer = this.getPlayer(account);
        if (localPlayer != null) {
            NameTags.removeNameTag(localPlayer);
            NameTags.updateAllTags();
        }
    }
}
