package net.warcane.lugin.core.minecraft.menu.pagination;

import com.google.common.collect.Lists;
import net.warcane.lugin.core.minecraft.menu.SimpleMenu;
import net.warcane.lugin.core.minecraft.menu.SimpleMenuManager;
import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import net.warcane.lugin.core.minecraft.task.Tasks;
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

@Slf4j
public class DynamicPaginationContext<T> extends MenuPaginationContext<T> {
    private Holder<T> holder;
    private boolean isLoading = false;
    
    public DynamicPaginationContext(Player player, Map<String, Object> rawData, MenuConfig menuConfig, SimpleMenu menu, SimpleMenuManager manager) {
        super(player, rawData, menuConfig, menu, manager);
    }
    
    public void setPagination(char key, Holder<T> holder, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        if (menuConfig.getLayout() == null) {
            throw new IllegalStateException("Menu layout is not defined.");
        }
        this.setPagination(menuConfig.getLayout().get(key), holder, itemRenderer, clickHandler);
    }
    public void setPagination(int[] slots, Holder<T> holder, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        this.slots = IntStream.of(slots).boxed().toList();
        this.holder = holder;
        this.renderer = itemRenderer;
        this.clickHandler = clickHandler;
        this.currentPage = 0;
        this.refreshData();
    }
    
    public void refreshData() {
        if (this.holder == null) {
            throw new IllegalStateException("Data supplier for dynamic pagination is not set.");
        }
        if (this.isLoading) { return; }
        
        this.isLoading = true;
        this.holder.dataSupplier.get()
            .thenComposeAsync(objects -> {
                this.isLoading = false;
                if (objects == null) {
                    objects = List.of();
                }
                
                objects = this.holder.onSuccess.apply(objects);
                
                this.pages = Lists.partition(objects, this.slots.size());
                if (this.currentPage >= this.pages.size()) {
                    this.currentPage = Math.max(0, this.pages.size() - 1);
                }
                
                return CompletableFuture.completedFuture(objects);
            }, Tasks::runAsync)
            .thenAcceptAsync(ts -> {
                for (int slot : this.slots) {
                    this.items.remove(slot);
                }
                super.update();
            }, Tasks::runSync)
            .exceptionallyAsync(throwable -> {
                this.isLoading = false;
                this.holder.onError.accept(throwable);
                log.error("Failed to fetch data for dynamic pagination menu", throwable);
                return null;
            }, Tasks::runAsync);
    }
    
    public record Holder<T>(Supplier<CompletableFuture<List<T>>> dataSupplier, Function<List<T>, List<T>> onSuccess, Consumer<Throwable> onError) {
        public static <T> Holder<T> of(Supplier<CompletableFuture<List<T>>> dataSupplier) {
            return new Holder<>(dataSupplier, Function.identity(), throwable -> {});
        }
        
        public static <T> Holder<T> of(Supplier<CompletableFuture<List<T>>> dataSupplier, Function<List<T>, List<T>> onSuccess, Consumer<Throwable> onError) {
            return new Holder<>(dataSupplier, onSuccess, onError);
        }
    }
}
