package net.warcane.lugin.core.minecraft.menu.pagination;

import com.google.common.collect.Lists;
import net.warcane.lugin.core.minecraft.menu.PlayerMenuContext;
import net.warcane.lugin.core.minecraft.menu.SimpleMenu;
import net.warcane.lugin.core.minecraft.menu.SimpleMenuManager;
import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import net.warcane.lugin.core.minecraft.menu.item.SimpleMenuItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class MenuPaginationContext<T> extends PlayerMenuContext {
    protected List<Integer> slots = new ArrayList<>();
    protected int currentPage = 0;

    protected List<List<T>> pages = new ArrayList<>();
    protected BiFunction<Player, T, ItemStack> renderer = (p, t) -> ItemStack.empty();
    protected BiConsumer<T, InventoryClickEvent> clickHandler = (t, e) -> e.setCancelled(true);

    public MenuPaginationContext(Player player, Map<String, Object> rawData, MenuConfig menuConfig, SimpleMenu menu, SimpleMenuManager manager) {
        super(player, rawData, menuConfig, menu, manager);
    }
    
    @Override
    public void update() {
        List<T> pageItems = this.getCurrentPageItems();
        int toRender = Math.min(pageItems.size(), slots.size());
        
        for (int i = 0; i < toRender; i++) {
            T item = pageItems.get(i);
            this.items.put(this.slots.get(i), new SimpleMenuItem(
                p -> this.renderer.apply(p, item),
                event -> this.clickHandler.accept(item, event)
            ));
        }
        for (int i = toRender; i < slots.size(); i++) {
            this.items.put(this.slots.get(i), new SimpleMenuItem(p -> AIR, event -> event.setCancelled(true)));
        }
        
        super.update();
    }
    
    private List<T> getCurrentPageItems() {
        try {
            int pageIndex = Math.clamp(this.currentPage, 0, Math.max(0, this.pages.size() - 1));
            return this.pages.get(pageIndex);
        } catch (IndexOutOfBoundsException e) {
            return Collections.emptyList();
        }
    }

    public void setPagination(char key, List<T> listObjects, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        if (menuConfig.getLayout() == null) {
            throw new IllegalStateException("Menu layout is not defined.");
        }
        this.setPagination(menuConfig.getLayout().get(key), listObjects, itemRenderer, clickHandler);
    }
    public void setPagination(int[] slots, List<T> listObjects, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        this.slots = IntStream.of(slots).boxed().toList();
        this.pages = Lists.partition(listObjects, slots.length);
        this.renderer = itemRenderer;
        this.clickHandler = clickHandler;
        this.currentPage = 0;
        update();
    }

    public void setPreviousButton(char key, ItemStack stack) {
        if (menuConfig.getLayout() == null) {
            throw new IllegalStateException("Menu layout is not defined.");
        }
        this.setPreviousButton(menuConfig.getLayout().get(key), stack);
    }
    public void setPreviousButton(int slot, ItemStack stack) {
        this.setPreviousButton(new int[] { slot }, stack);
    }
    public void setPreviousButton(int[] slot, ItemStack stack) {
        for (int i : slot) {
            items.put(i, new SimpleMenuItem(
                p -> hasPreviousPage() ? stack : AIR,
                event -> previousPage()
            ));
        }
    }

    public void setNextButton(char key, ItemStack stack) {
        if (menuConfig.getLayout() == null) {
            throw new IllegalStateException("Menu layout is not defined.");
        }
        this.setNextButton(menuConfig.getLayout().get(key), stack);
    }
    public void setNextButton(int slot, ItemStack stack) {
        this.setNextButton(new int[] { slot }, stack);
    }
    public void setNextButton(int[] slot, ItemStack stack) {
        for (int i : slot) {
            items.put(i, new SimpleMenuItem(
                p -> hasNextPage() ? stack : AIR,
                event -> nextPage()
            ));
        }
    }

    public boolean hasPreviousPage() {
        return this.currentPage > 0;
    }

    public boolean hasNextPage() {
        return this.slots != null && !this.slots.isEmpty() && this.currentPage < this.pages.size() - 1;
    }

    public void previousPage() {
        if (hasPreviousPage()) {
            this.currentPage--;
            update();
        }
    }
    
    public void nextPage() {
        if (hasNextPage()) {
            this.currentPage++;
            update();
        }
    }
}
