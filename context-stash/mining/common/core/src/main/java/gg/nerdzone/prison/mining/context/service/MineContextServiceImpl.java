/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.context.service;

import gg.nerdzone.prison.mining.api.context.MineBlockBreakContext;
import gg.nerdzone.prison.mining.api.context.MineContextPostHandler;
import gg.nerdzone.prison.mining.api.context.MineContextService;
import gg.nerdzone.prison.mining.api.context.state.MineContextState.StatePriority;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of {@link MineContextService}.
 */
@NoArgsConstructor
public class MineContextServiceImpl implements MineContextService {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

    private final Logger logger = Logger.getLogger(MineContextServiceImpl.class.getSimpleName());

    private final Map<MineContextPostHandlerId, MineContextPostHandler> handlerMap = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<MineBlockBreakContext> contextsQueue = new ConcurrentLinkedQueue<>();

    public static void shutdown() {
        EXECUTOR_SERVICE.shutdownNow();
    }

    @Override
    public void submitContext(@NotNull MineBlockBreakContext context) {
        // Pooling logic
        EXECUTOR_SERVICE.execute(() -> {
            try {
                synchronized (context.getMine()) {
                    this.handlerMap.forEach((identifier, handler) -> this.postContext(context, identifier, handler));
                    this.handlePostContext(context);
                }
            } catch (Throwable throwable) {
                this.logger.log(Level.SEVERE, "Error handling post context", throwable);
            }
        });
    }

    @Override
    public @NotNull MineContextPostHandler registerPostHandler(@NonNull MineContextService.MineContextPostHandlerId identifier, @NonNull MineContextPostHandler handler) {
        return this.handlerMap.compute(identifier, ($, existingHandler) -> Objects.requireNonNullElse(existingHandler, handler));
    }

    @Override
    public @Nullable MineContextPostHandler unregisterPostHandler(@NonNull MineContextService.MineContextPostHandlerId identifier) {
        return this.handlerMap.remove(identifier);
    }

    @Override
    public void handlePostContext(@NotNull MineBlockBreakContext context) {
        context.complete(StatePriority.HIGHEST, this::flush);
    }

    /**
     * Register the post-handler for the context.
     *
     * @param context    The context to register the post-handler for
     * @param identifier The identifier for the post-handler
     * @param handler    The post-handler to register
     */
    @Internal
    private void postContext(@NotNull MineBlockBreakContext context, @NotNull MineContextService.MineContextPostHandlerId identifier, @NotNull MineContextPostHandler handler) {
        context.getState().whenComplete(
            identifier, (ctx) -> {
                try {
                    handler.handle(ctx);
                } catch (Throwable throwable) {
                    this.logger.log(
                        Level.WARNING,
                        "Error in post-handler [%s] for mine: %s, blocks: %d".formatted(identifier, ctx.getMine().getMineId(), ctx.getBlocksCount()),
                        throwable
                    );
                }
            }
        );
    }

    @Internal
    private void flush(@NotNull MineBlockBreakContext context) {
        if (!context.isAutoSendOnFlush()) { // Ignore if auto-send is disabled
            return;
        }

        try {
            // Apply changes
            context.sendBlocks(true, false, false);
        } finally {
            // Flush the context datas
            context.flush();
        }
    }
}
