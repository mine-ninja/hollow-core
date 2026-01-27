package io.github.minehollow.minecraft.placeholder;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

public interface TextPlaceholder {

    static TextPlaceholder playerPlaceholder(@NotNull String tag, @NotNull Function<Player, String> playerFunction) {
        return new PlayerTextPlaceholder(tag, playerFunction);
    }

    static TextPlaceholder globalPlaceholder(@NotNull String tag, @NotNull Supplier<String> valueSupplier) {
        return new GlobalTextPlaceholder(tag, valueSupplier);
    }

    /**
     * A tag do placeholder.
     *
     * @return o tag do placeholder
     */
    String getTag();
}
