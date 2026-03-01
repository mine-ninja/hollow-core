package io.github.minehollow.minecraft.event;

import io.github.minehollow.minecraft.BukkitPlatformPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MultiEventBuilder<T extends Event> {
    private final Class<T> mainType;
    private final Class<? extends T>[] eventClasses;
    private EventPriority priority = EventPriority.NORMAL;
    private boolean ignoreCancelled = false;
    private Predicate<T> filter = e -> true;
    private int expireAfter = -1;

    @SafeVarargs
    public MultiEventBuilder(Class<T> mainType, Class<? extends T>... eventClasses) {
        this.mainType = mainType;
        this.eventClasses = eventClasses;
    }

    public MultiEventBuilder<T> priority(EventPriority priority) {
        this.priority = priority;
        return this;
    }

    public MultiEventBuilder<T> ignoreCancelled(boolean ignore) {
        this.ignoreCancelled = ignore;
        return this;
    }

    public MultiEventBuilder<T> filter(@NotNull Predicate<T> filter) {
        this.filter = this.filter.and(filter);
        return this;
    }

    public MultiEventBuilder<T> expireAfter(int instances) {
        this.expireAfter = instances;
        return this;
    }

    public void handler(@NotNull Consumer<T> handler) {
        Listener listener = new Listener() {};
        final int[] remaining = {expireAfter};
        for (Class<? extends T> eventClass : eventClasses) {
            EventExecutor executor = (l, event) -> {
                if (!mainType.isInstance(event)) return;
                T e = mainType.cast(event);
                if (ignoreCancelled && event instanceof Cancellable && ((Cancellable) event).isCancelled()) return;
                if (!filter.test(e)) return;
                handler.accept(e);
                if (remaining[0] > 0) {
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        HandlerList.unregisterAll(listener);
                    }
                }
            };
            Bukkit.getPluginManager().registerEvent(eventClass, listener, priority, executor, BukkitPlatformPlugin.getInstance());
        }
    }
}
