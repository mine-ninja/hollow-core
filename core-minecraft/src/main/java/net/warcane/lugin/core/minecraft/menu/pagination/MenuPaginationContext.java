package net.warcane.lugin.core.minecraft.menu.pagination;

import net.warcane.lugin.core.minecraft.menu.PlayerMenuContext;
import net.warcane.lugin.core.minecraft.menu.SimpleMenu;
import net.warcane.lugin.core.minecraft.menu.SimpleMenuManager;
import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import net.warcane.lugin.core.minecraft.menu.item.SimpleMenuItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.IntStream;

//
public class MenuPaginationContext<T> extends PlayerMenuContext {

    private List<Integer> paginationSlots;
    private int currentPage = 0;

    private List<T> currentListObjects;
    private BiFunction<Player, T, ItemStack> currentItemRenderer;
    private BiConsumer<T, InventoryClickEvent> currentClickHandler;

    public MenuPaginationContext(Player player, Map<String, Object> rawData, MenuConfig menuConfig, SimpleMenu menu, SimpleMenuManager manager) {
        super(player, rawData, menuConfig, menu, manager);
    }

    public void setPagination(int[] slots, List<T> listObjects, @NotNull BiFunction<Player, T, ItemStack> itemRenderer, @NotNull BiConsumer<T, InventoryClickEvent> clickHandler) {
        this.paginationSlots = IntStream.of(slots).boxed().toList();
        this.currentListObjects = listObjects;
        this.currentItemRenderer = itemRenderer;
        this.currentClickHandler = clickHandler;
        this.currentPage = 0;
        update();
    }

    @Override
    public void update() {
        int startIndex = currentPage * paginationSlots.size();
        for (int i = 0; i < paginationSlots.size(); i++) {
            int slot = paginationSlots.get(i);
            int listIndex = startIndex + i;
            if (listIndex < currentListObjects.size()) {
                T obj = currentListObjects.get(listIndex);
                ItemStack itemStack = currentItemRenderer.apply(player, obj);
                Consumer<InventoryClickEvent> eventConsumer = event -> currentClickHandler.accept(obj, event);
                items.put(slot, new SimpleMenuItem(itemStack, eventConsumer));
            } else {
                items.put(slot, new SimpleMenuItem(AIR));
            }
        }
        super.update();
    }

    public void setPreviousButton(int slot, ItemStack item) {
        items.put(slot, new SimpleMenuItem((player) -> {
            if (!hasPreviousPage()) return AIR;
            return item;
        }, event -> previousPage()));
    }

    public void setNextButton(int slot, ItemStack itemSupplier) {
        items.put(slot, new SimpleMenuItem((player) -> {
            if (!hasNextPage()) return AIR;
            return itemSupplier;
        }, event -> nextPage()));
    }

    public boolean hasPreviousPage() {
        return currentPage > 0;
    }

    public boolean hasNextPage() {
        if (paginationSlots == null || paginationSlots.isEmpty()) {
            return false;
        }
        int maxPage = (int) Math.ceil((double) currentListObjects.size() / paginationSlots.size()) - 1;
        return currentPage < maxPage;
    }

    public void previousPage() {
        if (paginationSlots == null || paginationSlots.isEmpty()) {
            throw new IllegalStateException("Pagination slots not set.");
        }
        if (currentPage > 0) {
            currentPage--;
            update();
        }
    }


    public void nextPage() {
        if (paginationSlots == null || paginationSlots.isEmpty()) {
            throw new IllegalStateException("Pagination slots not set.");
        }
        int maxPage = (int) Math.ceil((double) items.size() / paginationSlots.size()) - 1;
        if (currentPage < maxPage) {
            currentPage++;
            update();
        }
    }
}
