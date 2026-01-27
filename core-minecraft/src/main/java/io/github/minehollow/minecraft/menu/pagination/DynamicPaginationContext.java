package io.github.minehollow.minecraft.menu.pagination;

import com.google.common.collect.Lists;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.SimpleMenuManager;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.task.Tasks;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * A dynamic pagination context for managing paginated menus in Minecraft.
 * This class handles the rendering and interaction of paginated menu items dynamically.
 *
 * @param <T> The type of data being paginated.
 */
@Slf4j
public class DynamicPaginationContext<T> extends MenuPaginationContext<T> {
    private Holder<T> holder;
    private boolean isLoading = false;
    
    /**
     * Constructs a new DynamicPaginationContext.
     *
     * @param player     The player interacting with the menu.
     * @param rawData    The raw data associated with the menu.
     * @param menuConfig The configuration of the menu.
     * @param menu       The menu instance.
     * @param manager    The menu manager.
     */
    public DynamicPaginationContext(Player player, Map<String, Object> rawData, MenuConfig menuConfig, SimpleMenu menu, SimpleMenuManager manager) {
        super(player, rawData, menuConfig, menu, manager);
    }
    
    /**
     * Sets up pagination using a layout key.
     *
     * @param key          The layout key for pagination slots.
     * @param holder       The data holder for pagination.
     * @param itemRenderer A function to render items for each data entry.
     * @param clickHandler A consumer to handle click events on items.
     */
    public void setPagination(char key, Holder<T> holder, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        if (menuConfig.getLayout() == null) {
            throw new IllegalStateException("Menu layout is not defined.");
        }
        this.setPagination(menuConfig.getLayout().get(key), holder, itemRenderer, clickHandler);
    }
    
    /**
     * Sets up pagination using specific slots.
     *
     * @param slots        The slots to use for pagination.
     * @param holder       The data holder for pagination.
     * @param itemRenderer A function to render items for each data entry.
     * @param clickHandler A consumer to handle click events on items.
     */
    public void setPagination(int[] slots, Holder<T> holder, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        this.slots = IntStream.of(slots).boxed().toList();
        this.holder = holder;
        this.renderer = itemRenderer;
        this.clickHandler = clickHandler;
        this.currentPage = 0;
        this.refreshData();
    }
    
    /**
     * Refreshes the data for the pagination, fetching it asynchronously.
     */
    public void refreshData() {
        if (this.holder == null) {
            throw new IllegalStateException("Data supplier for dynamic pagination is not set.");
        }
        if (this.isLoading) { return; }
        
        this.isLoading = true;
        this.holder.supplier.get()
            .thenApplyAsync(this::handleSuccess, Tasks::runAsync)
            .handleAsync(this::handleComplete, Tasks::runSync);
    }
    
    /**
     * Handles the successful fetching of data.
     *
     * @param data The fetched data.
     *
     * @return The processed data.
     */
    private List<T> handleSuccess(List<T> data) {
        if (data == null) data = List.of();
        data = this.holder.onSuccess.apply(data);
        
        this.pages = Lists.partition(data, this.slots.size());
        if (this.currentPage >= this.pages.size()) {
            this.currentPage = Math.max(0, this.pages.size() - 1);
        }
        
        return data;
    }
    
    /**
     * Handles the completion of data fetching, whether successful or with an error.
     *
     * @param data      The fetched data.
     * @param throwable The exception thrown during fetching, if any.
     *
     * @return Always returns null.
     */
    private Void handleComplete(List<T> data, Throwable throwable) {
        this.isLoading = false;
        if (throwable != null) {
            this.holder.onError.accept(throwable);
            log.error("Failed to fetch data for dynamic pagination menu", throwable);
        } else {
            for (int slot : this.slots) {
                this.items.remove(slot);
            }
            this.holder.onComplete.accept(data);
            super.update();
        }
        return null;
    }
    
    /**
     * A record representing a holder for pagination data and its associated callbacks.
     *
     * @param <T> The type of data being held.
     */
    public record Holder<T>(Supplier<CompletableFuture<List<T>>> supplier, Function<List<T>, List<T>> onSuccess, Consumer<List<T>> onComplete, Consumer<Throwable> onError) {
        /**
         * Creates a Holder with a data supplier.
         *
         * @param dataSupplier The supplier for fetching data.
         * @param <T>          The type of data.
         *
         * @return A new Holder instance.
         */
        public static <T> Holder<T> of(Supplier<CompletableFuture<List<T>>> dataSupplier) {
            return new Holder<>(dataSupplier, Function.identity(), data -> { }, throwable -> { });
        }
        
        /**
         * Creates a Holder with a data supplier and a success handler.
         *
         * @param supplier  The supplier for fetching data.
         * @param onSuccess A function to process data on success.
         * @param <T>       The type of data.
         *
         * @return A new Holder instance.
         */
        public static <T> Holder<T> of(Supplier<CompletableFuture<List<T>>> supplier, Function<List<T>, List<T>> onSuccess) {
            return new Holder<>(supplier, onSuccess, data -> { }, throwable -> { });
        }
        
        /**
         * Creates a Holder with a data supplier and a completion handler.
         *
         * @param supplier   The supplier for fetching data.
         * @param onComplete A consumer to handle data on completion.
         * @param <T>        The type of data.
         *
         * @return A new Holder instance.
         */
        public static <T> Holder<T> of(Supplier<CompletableFuture<List<T>>> supplier, Consumer<List<T>> onComplete) {
            return new Holder<>(supplier, Function.identity(), onComplete, throwable -> { });
        }
        
        /**
         * Creates a Holder with a data supplier, success handler, and completion handler.
         *
         * @param supplier   The supplier for fetching data.
         * @param onSuccess  A function to process data on success.
         * @param onComplete A consumer to handle data on completion.
         * @param <T>        The type of data.
         *
         * @return A new Holder instance.
         */
        public static <T> Holder<T> of(Supplier<CompletableFuture<List<T>>> supplier, Function<List<T>, List<T>> onSuccess, Consumer<List<T>> onComplete) {
            return new Holder<>(supplier, onSuccess, onComplete, throwable -> { });
        }
        
        /**
         * Creates a Holder with a data supplier, success handler, completion handler, and error handler.
         *
         * @param supplier   The supplier for fetching data.
         * @param onSuccess  A function to process data on success.
         * @param onComplete A consumer to handle data on completion.
         * @param onError    A consumer to handle errors.
         * @param <T>        The type of data.
         *
         * @return A new Holder instance.
         */
        public static <T> Holder<T> of(Supplier<CompletableFuture<List<T>>> supplier, Function<List<T>, List<T>> onSuccess, Consumer<List<T>> onComplete, Consumer<Throwable> onError) {
            return new Holder<>(supplier, onSuccess, onComplete, onError);
        }
    }
}
