package net.warcane.lugin.core.minecraft.nametag;

import com.github.retrooper.packetevents.util.ColorUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.warcane.lugin.core.player.account.PlayerAccount;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModernNameTagResolver extends NameTagResolver {
    // stric força o minimessage a fechar as tags
    private final MiniMessage miniMessage = MiniMessage.builder().strict(true).build();
    
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
        .character('&').hexColors().useUnusualXRepeatedCharacterHexFormat().build();
    
    @Override
    public void applyNameTag(@NotNull PlayerAccount account) {
        final var localPlayer = this.getPlayer(account);
        if (localPlayer == null) return;
        
        final var group = account.getHighestSubscription().group();
        var groupPrefix = group.getPrefix();
        
        if (group.getModernTag() != ' ') {
            groupPrefix = parse(Component.text(group.getModernTag() + " ", Style.empty().font(Key.key("lugin:tags")).color(TextColor.color(0xFFFFFF))));
        }
        
        NameTag nameTag = PLAYER_TAGS.get(localPlayer.getName());
        if (nameTag != null) {
            removeNameTag(localPlayer);
        }
        
        setNameTag(localPlayer, new NameTag(groupPrefix, "", ColorUtil.toString(group.getNamedTextColor())));
        updateAllTags();
    }
    
    @Override
    public void updateAllTags() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            NameTag team = PLAYER_TAGS.get(player.getName());
            if (team != null) {
                setNameTag(player, team);
            }
        }
    }
    
    public void setPrefix(@NotNull Player player, Component prefix) throws UnsupportedOperationException {
        this.setPrefix(player, parse(prefix));
    }
    
    public void setSuffix(@NotNull Player player, Component suffix) throws UnsupportedOperationException {
        this.setSuffix(player, parse(suffix));
    }
    
    public void setColor(@NotNull Player player, @Nullable NamedTextColor color) throws UnsupportedOperationException {
        this.setColor(player, color != null ? ColorUtil.toString(color) : null);
    }
    
    private String parse(Component component) {
        try {
            return miniMessage.serialize(component).replace("§", "&");
        } catch (Exception e) {
            try {
                TextComponent deserialize = LEGACY.deserialize(LEGACY.serialize(component));
                return miniMessage.serialize(deserialize).replace("§", "&");
            } catch (Exception ignored) { }
        }
        return null;
    }
}
