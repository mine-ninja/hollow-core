package io.github.minehollow.minecraft.placeholder;

import lombok.Data;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

@Data
public class PlayerTextPlaceholder implements TextPlaceholder {

    private final String tag;
    private final Function<Player, String> placeholderFunction;

    @NotNull
    @Override
    public String getTag() {
        return tag;
    }
}
