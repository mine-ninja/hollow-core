package io.github.minehollow.minecraft.nametag.resolver;

import com.github.retrooper.packetevents.util.ColorUtil;
import io.github.minehollow.minecraft.BukkitPlatform;
import io.github.minehollow.sdk.group.PlayerGroup;
import io.github.minehollow.sdk.player.account.PlayerAccount;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.function.Function;

public class ModernNameTagResolver extends NameTagResolver {
    // stric força o minimessage a fechar as tags
    private final MiniMessage miniMessage = MiniMessage.builder().strict(true).build();

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
      .character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public ModernNameTagResolver(BukkitPlatform platform) {
        this.tagPrefix = (player -> {
            PlayerAccount account = platform.getPlayerAccountService().getCachedAccount(player.getUniqueId());
            PlayerGroup group = account.getHighestSubscription().group();
            if (group.getModernTag() != ' ') {
                return parse(Component.text(group.getModernTag() + " ", Style.empty().font(Key.key("hollow:tags")).color(NamedTextColor.WHITE)));
            }
            return "";
        });
        this.tagColor = (player -> {
            PlayerAccount account = platform.getPlayerAccountService().getCachedAccount(player.getUniqueId());
            return ColorUtil.toString(account.getHighestSubscription().group().getNamedTextColor());
        });
    }

    public void setPrefix(Function<Player, Component> prefix) {
        this.tagPrefix = (player -> {
            Component component = prefix.apply(player);
            return parse(component);
        });
    }

    public void setSuffix(Function<Player, Component> suffix) {
        this.tagSuffix = (player -> {
            Component component = suffix.apply(player);
            return parse(component);
        });
    }

    public void setColor(Function<Player, NamedTextColor> color) {
        this.tagColor = (player -> {
            NamedTextColor namedTextColor = color.apply(player);
            return ColorUtil.toString(namedTextColor);
        });
    }

    private String parse(Component component) {
        try {
            return miniMessage.serialize(component).replace("§", "&");
        } catch (Exception e) {
            try {
                TextComponent deserialize = LEGACY.deserialize(LEGACY.serialize(component));
                return miniMessage.serialize(deserialize).replace("§", "&");
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
