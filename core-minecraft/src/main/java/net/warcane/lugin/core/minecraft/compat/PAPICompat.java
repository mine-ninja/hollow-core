package net.warcane.lugin.core.minecraft.compat;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.nametag.NameTagResolver;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PAPICompat extends PlaceholderExpansion {
    private final BukkitPlatformPlugin plugin;
    private final NameTagResolver resolver;
    
    public PAPICompat(BukkitPlatformPlugin plugin, NameTagResolver resolver) {
        this.plugin = plugin;
        this.resolver = resolver;
    }
    
    @Override @Nullable
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return super.onPlaceholderRequest(player, params);
        
        switch (params) {
            case "player_prefix" -> {
                return this.resolver.getTagPrefix(player) + this.resolver.getTagColor(player);
            }
            case "player_suffix" -> {
                return this.resolver.getTagSuffix(player);
            }
        }
        
        return super.onPlaceholderRequest(player, params);
    }
    
    @Override @NotNull
    public String getIdentifier() {
        return "lugin-core";
    }
    
    @Override @NotNull
    public String getAuthor() {
        return String.join(", ", this.plugin.getDescription().getAuthors());
    }
    
    @Override @NotNull
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }
}
