package net.warcane.lugin.core.minecraft.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Rok, Pedro Lucas nmm. Created on 03/11/2025
 * @project LUGIN
 */
public class LuginPapiExpansion extends PlaceholderExpansion {

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String getIdentifier() {
        return "lugin"; // %lugin_<key>%
    }

    @Override
    public String getAuthor() {
        return "Lugin Team";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }


    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        TextPlaceholder placeholderByTag = PlaceholderMap.getPlaceholderByTag(params);
        return switch (placeholderByTag) {
            case null -> null;

            case PlayerTextPlaceholder playerTextPlaceholder -> {
                if (player == null) yield null;
                yield playerTextPlaceholder.getPlaceholderFunction().apply(player);
            }

            case GlobalTextPlaceholder globalTextPlaceholder ->
                globalTextPlaceholder.getPlaceholderSupplier().get();
            default -> super.onPlaceholderRequest(player, params);
        };
    }
}
