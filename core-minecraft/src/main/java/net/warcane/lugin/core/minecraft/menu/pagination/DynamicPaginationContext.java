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
import java.util.stream.IntStream;

@Slf4j
public class DynamicPaginationContext<T> extends MenuPaginationContext<T> {
    private CompletableFuture<List<T>> future;
    private boolean autoRebuild = false;
    
    public DynamicPaginationContext(Player player, Map<String, Object> rawData, MenuConfig menuConfig, SimpleMenu menu, SimpleMenuManager manager) {
        super(player, rawData, menuConfig, menu, manager);
    }
    
    @Override
    public void update() {
        if (autoRebuild) this.refreshPages(false);
        super.update();
    }
    
    public void autoRebuild(boolean autoRebuild) {
        this.autoRebuild = autoRebuild;
    }
    
    public void setPagination(char key, CompletableFuture<List<T>> future, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        if (menuConfig.getLayout() == null) {
            throw new IllegalStateException("Menu layout is not defined.");
        }
        this.setPagination(menuConfig.getLayout().get(key), future, itemRenderer, clickHandler);
    }
    public void setPagination(int[] slots, CompletableFuture<List<T>> future, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        this.slots = IntStream.of(slots).boxed().toList();
        this.future = future;
        this.renderer = itemRenderer;
        this.clickHandler = clickHandler;
        this.currentPage = 0;
        
        this.refreshPages(true);
    }
    
    public void refreshPages(boolean update) {
        if (this.future == null) {
            throw new IllegalStateException("Supplier for dynamic pagination is not set.");
        }
        
        this.future
            .whenCompleteAsync((objects, throwable) -> {
                if (throwable != null) {
                    log.error("Error fetching pagination items", throwable);
                    return;
                }
                if (objects == null) {
                    objects = List.of();
                }
                this.pages = Lists.partition(objects, this.slots.size());
            }, Tasks::runAsync)
            .thenAcceptAsync(ts -> {
                if (update) {
                    this.update();
                }
            }, Tasks::runSync);
    }
}
