package io.github.minehollow.minecraft.placeholder;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

@Data
public class GlobalTextPlaceholder implements TextPlaceholder {

    private final String tag;
    private final Supplier<String> placeholderSupplier;

    public GlobalTextPlaceholder(String tag, Supplier<String> placeholderSupplier) {
        this.tag = tag;
        this.placeholderSupplier = placeholderSupplier;
    }

    @NotNull
    @Override
    public String getTag() {
        return tag;
    }


}
