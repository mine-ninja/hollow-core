package io.github.minehollow.minecraft.event.tick;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AsyncServerTickEvent extends Event {

    // não tem por que instanciar esse evento....
    private static final AsyncServerTickEvent INSTANCE = new AsyncServerTickEvent();

    public static void call() {
        INSTANCE.callEvent();
    }

    @Getter
    private static final HandlerList handlerList = new HandlerList();


    public AsyncServerTickEvent() {
        super(true);
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlerList;
    }
}
