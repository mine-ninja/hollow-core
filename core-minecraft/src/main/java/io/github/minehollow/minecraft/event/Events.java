package io.github.minehollow.minecraft.event;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public class Events {

    public static <T extends Event> EventBuilder<T> subscribe(
        @NotNull Class<T> eventClass
    ) {
        return new EventBuilder<>(eventClass);
    }


    @SafeVarargs
    public static <T extends Event> MultiEventBuilder<T> subscribeMultiple(
        @NotNull Class<T> mainType,
        @NotNull Class<? extends T>... eventClasses
    ) {
        return new MultiEventBuilder<>(mainType, eventClasses);
    }
}
