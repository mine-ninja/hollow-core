/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.context.model;

import gg.nerdzone.prison.mining.api.context.MineBlockBreakContext;
import gg.nerdzone.prison.mining.api.context.MineBlockContextConsumer;
import gg.nerdzone.prison.mining.api.context.state.MineContextFutureState;
import gg.nerdzone.prison.mining.api.context.state.MineContextState;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

/**
 * @see MineBlockBreakContext
 * @see MineContextState
 */
public class MineContextStateImpl implements MineContextState {

    private static final Logger LOGGER = Logger.getLogger(MineContextStateImpl.class.getSimpleName()); // Exception tracing

    private final MineBlockBreakContext context; // Circular reference

    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicInteger awaiting = new AtomicInteger(0);

    private final PriorityBlockingQueue<ConsumerEntry> completeConsumers = new PriorityBlockingQueue<>();

    protected MineContextStateImpl(@NotNull MineBlockBreakContext context) {
        this.context = context;
    }

    @Override
    public @NotNull MineBlockBreakContext getContext() {
        return this.context;
    }

    @Override
    public void await(@NotNull MineContextFutureState stateFuture) {
        if (stateFuture.isDone()) {
            return;
        }

        this.awaiting.incrementAndGet();

        stateFuture.whenComplete(($, $2) -> {
            if (this.awaiting.decrementAndGet() == 0) {
                this.complete();
            }
        });
    }

    @Override
    @SneakyThrows(Throwable.class)
    public void whenComplete(@NonNull Object identifier, @NonNull MineContextState.StatePriority priority, @NonNull MineBlockContextConsumer consumer) {
        if (this.isCompleted()) {
            consumer.accept(this.getContext());
            return;
        }

        this.completeConsumers.add(new ConsumerEntry(priority.getPriority(), consumer));
    }

    @Override
    public boolean complete() {
        if (this.awaiting.get() > 0) {
            return false;
        }

        if (!this.completed.compareAndSet(false, true)) {
            return false;
        }

        ConsumerEntry entry;
        while ((entry = this.completeConsumers.poll()) != null) {
            try {
                entry.consumer().accept(this.getContext());
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Exception in consumer [priority=" + entry.priority() + "]", t);
            }
        }

        return true;
    }

    @Override
    public boolean isCompleted() {
        return this.completed.get();
    }

    private record ConsumerEntry(int priority, @NotNull MineBlockContextConsumer consumer) implements Comparable<ConsumerEntry> {
        @Override
        public int compareTo(ConsumerEntry entry) {
            return Integer.compare(this.priority, entry.priority);
        }
    }
}
