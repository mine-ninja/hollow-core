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
import java.util.function.Supplier;
import java.util.stream.IntStream;

@Slf4j
public class DynamicPaginationContext<T> extends MenuPaginationContext<T> {
    private Supplier<CompletableFuture<List<T>>> dataSupplier;
    private boolean isLoading = false;
    
    public DynamicPaginationContext(Player player, Map<String, Object> rawData, MenuConfig menuConfig, SimpleMenu menu, SimpleMenuManager manager) {
        super(player, rawData, menuConfig, menu, manager);
    }
    
    public void setPagination(char key, Supplier<CompletableFuture<List<T>>> dataSupplier, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        if (menuConfig.getLayout() == null) {
            throw new IllegalStateException("Menu layout is not defined.");
        }
        this.setPagination(menuConfig.getLayout().get(key), dataSupplier, itemRenderer, clickHandler);
    }
    public void setPagination(int[] slots, Supplier<CompletableFuture<List<T>>> dataSupplier, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        this.slots = IntStream.of(slots).boxed().toList();
        this.dataSupplier = dataSupplier;
        this.renderer = itemRenderer;
        this.clickHandler = clickHandler;
        this.currentPage = 0;
        this.refreshData();
    }
    
    public void refreshData() {
        if (this.dataSupplier == null) {
            throw new IllegalStateException("Data supplier for dynamic pagination is not set.");
        }
        if (this.isLoading) { return; }
        
        this.isLoading = true;
        this.dataSupplier.get()
            .whenCompleteAsync((objects, throwable) -> {
                this.isLoading = false;
                if (throwable != null) {
                    log.error("Error fetching pagination items", throwable);
                    return;
                }
                if (objects == null) {
                    objects = List.of();
                }
                
                this.pages = Lists.partition(objects, this.slots.size());
                if (this.currentPage >= this.pages.size()) {
                    this.currentPage = Math.max(0, this.pages.size() - 1);
                }
            }, Tasks::runAsync)
            .thenAcceptAsync(ts -> {
                for (int slot : this.slots) {
                    this.items.remove(slot);
                }
                super.update();
            }, Tasks::runSync);
    }
}
