package net.warcane.lugin.core.minecraft.menu;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import net.warcane.lugin.core.minecraft.menu.input.SignInputContext;
import net.warcane.lugin.core.minecraft.menu.input.SignInputMenu;
import net.warcane.lugin.core.minecraft.menu.item.SimpleMenuItem;
import net.warcane.lugin.core.minecraft.util.inventory.InventoryUpdate;
import net.warcane.lugin.core.minecraft.util.stopwatch.Stopwatch;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class PlayerMenuContext implements MenuContext {

    protected static final ItemStack AIR = new ItemStack(Material.AIR);

    protected final Player player;
    protected final Map<String, Object> rawData;
    protected final MenuConfig menuConfig;
    protected final SimpleMenu menu;
    protected final SimpleMenuManager manager;

    protected Inventory inventory;
    protected boolean onSignInput = false;
    protected final Int2ObjectMap<SimpleMenuItem> items = new Int2ObjectOpenHashMap<>();

    @Getter
    protected final Stopwatch stopwatch = new Stopwatch();

    public PlayerMenuContext(Player player, Map<String, Object> rawData, MenuConfig menuConfig, SimpleMenu menu, SimpleMenuManager manager) {
        this.player = player;
        this.rawData = new HashMap<>(rawData);
        this.menuConfig = menuConfig;
        this.menu = menu;
        this.manager = manager;
    }

    public void initialize() {
        if (isInitialized()) {
            throw new IllegalStateException("Inventory already initialized for this context.");
        }
        this.inventory = createInv();
    }

    public boolean isInitialized() {
        return inventory != null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull String key) {
        final var raw = rawData.get(key);
        return raw == null ? null : (T) raw;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(@NotNull String key, @NotNull T defaultValue) {
        final var raw = rawData.get(key);
        return raw == null ? defaultValue : (T) raw;
    }

    public void put(@NotNull String key, @NotNull Object value) {
        rawData.put(key, value);
    }

    public void remove(@NotNull String key) {
        rawData.remove(key);
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }

    @NotNull
    public SimpleMenu getMenu() {
        return menu;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @Nullable
    public SimpleMenuItem getItem(int slot) {
        return items.get(slot);
    }
    
    public void setItem(char key, @NotNull SimpleMenuItem menuItem) {
        if (menuConfig.getLayout() == null) throw new IllegalStateException("Menu layout isn't set, cannot use character keys.");
        setItem(menuConfig.getLayout().get(key), menuItem);
    }
    
    public void setItem(char key, @NotNull Function<Player, ItemStack> itemRenderer, @NotNull Consumer<InventoryClickEvent> clickHandler) {
        if (menuConfig.getLayout() == null) throw new IllegalStateException("Menu layout isn't set, cannot use character keys.");
        setItem(menuConfig.getLayout().get(key), itemRenderer, clickHandler);
    }
    
    public void setItem(char key, @NotNull ItemStack itemStack, @NotNull Consumer<InventoryClickEvent> clickHandler) {
        if (menuConfig.getLayout() == null) throw new IllegalStateException("Menu layout isn't set, cannot use character keys.");
        setItem(menuConfig.getLayout().get(key), itemStack, clickHandler);
    }
    
    public void setItem(char key, @NotNull ItemStack itemStack) {
        if (menuConfig.getLayout() == null) throw new IllegalStateException("Menu layout isn't set, cannot use character keys.");
        setItem(menuConfig.getLayout().get(key), itemStack);
    }
    
    public void setItem(char key, @NotNull Function<Player, ItemStack> itemRenderer) {
        if (menuConfig.getLayout() == null) throw new IllegalStateException("Menu layout isn't set, cannot use character keys.");
        setItem(menuConfig.getLayout().get(key), itemRenderer);
    }
    
    public void setItem(char key, @NotNull BiConsumer<Integer, SimpleMenuItem> itemSupplier) {
        if (menuConfig.getLayout() == null) throw new IllegalStateException("Menu layout isn't set, cannot use character keys.");
        int[] slots = menuConfig.getLayout().get(key);
        for (int i = 0; i < slots.length; i++) {
            SimpleMenuItem item = new SimpleMenuItem();
            itemSupplier.accept(i, item);
            setItem(slots[i], item);
        }
    }
    
    public void setItem(int[] slots, @NotNull SimpleMenuItem menuItem) {
        for (int slot : slots) {
            items.put(slot, menuItem);
        }
    }
    
    public void setItem(int[] slots, @NotNull Function<Player, ItemStack> itemRenderer, @NotNull Consumer<InventoryClickEvent> clickHandler) {
        for (int slot : slots) {
            items.put(slot, new SimpleMenuItem(itemRenderer, clickHandler));
        }
    }

    public void setItem(int[] slots, @NotNull ItemStack itemStack, @NotNull Consumer<InventoryClickEvent> clickHandler) {
        for (int slot : slots) {
            items.put(slot, new SimpleMenuItem(itemStack, clickHandler));
        }
    }

    public void setItem(int[] slots, @NotNull ItemStack itemStack) {
        for (int slot : slots) {
            items.put(slot, new SimpleMenuItem(itemStack));
        }
    }

    public void setItem(int[] slots, @NotNull Function<Player, ItemStack> itemRenderer) {
        for (int slot : slots) {
            items.put(slot, new SimpleMenuItem(itemRenderer));
        }
    }
    
    public void setItem(int slot, @NotNull SimpleMenuItem menuItem) {
        items.put(slot, menuItem);
    }

    public void setItem(int slot, @NotNull Function<Player, ItemStack> itemRenderer, @NotNull Consumer<InventoryClickEvent> clickHandler) {
        items.put(slot, new SimpleMenuItem(itemRenderer, clickHandler));
    }

    public void setItem(int slot, @NotNull Supplier<ItemStack> itemSupplier, @NotNull Consumer<InventoryClickEvent> clickHandler) {
        this.setItem(slot, player -> itemSupplier.get(), clickHandler);
    }

    public void setItem(int slot, @NotNull ItemStack itemStack, @NotNull Consumer<InventoryClickEvent> clickHandler) {
        items.put(slot, new SimpleMenuItem(itemStack, clickHandler));
    }

    public void setItem(int slot, @NotNull ItemStack itemStack) {
        items.put(slot, new SimpleMenuItem(itemStack));
    }

    public void setItem(int slot, @NotNull Function<Player, ItemStack> itemRenderer) {
        items.put(slot, new SimpleMenuItem(itemRenderer));
    }

    public void removeItem(int slot) {
        items.remove(slot);
        inventory.setItem(slot, AIR);
    }

    public void clearItems() {
        items.clear();
    }

    /**
     * Atualiza o contexto do menu para o jogador, atualizando os items no inventário
     * com os renderizadores associados a cada item.
     */
    @Override
    public void update() {
        for (var entry : items.int2ObjectEntrySet()) {
            var slot = entry.getIntKey();
            var item = entry.getValue();
            inventory.setItem(slot, item.renderer().apply(player));
        }
    }

    public void open() {
        if (!isInitialized()) {
            throw new IllegalStateException("Inventory not initialized for this context.");
        }

        items.forEach((slot, menuItem) -> {
            inventory.setItem(slot, menuItem.renderer().apply(player));
        });

        if (this.onSignInput) {
            this.onSignInput = false;
        }
        player.openInventory(inventory);
    }

    public void close() {
        player.closeInventory();
    }

    public void updateTitle(@NotNull String title) {
        InventoryUpdate.updateInventory(player, title);
    }

    public void updateTitle(@NotNull Component title) {
        InventoryUpdate.updateInventory(player, title);
    }

    public void openMenu(@NotNull Class<? extends SimpleMenu> clazz, boolean forwardInitialData) {
        openMenu(clazz, forwardInitialData, new HashMap<>());
    }

    public void openMenu(@NotNull Class<? extends SimpleMenu> clazz) {
        openMenu(clazz, false, new HashMap<>());
    }

    public void openMenu(@NotNull Class<? extends SimpleMenu> clazz, @NotNull Map<String, Object> initialData) {
        openMenu(clazz, false, initialData);
    }

    public void openMenu(@NotNull Class<? extends SimpleMenu> clazz, boolean forwardInitialData, @NotNull Map<String, Object> initialData) {
        if (forwardInitialData) {
            initialData.putAll(rawData);
        }
        manager.openToPlayer(player, clazz, initialData);
    }
    
    public void openSignInput(Consumer<SignInputContext> response) {
        openSignInput(response, new Component[0], false, new HashMap<>());
    }
    
    public void openSignInput(Consumer<SignInputContext> response, Component[] lines) {
        openSignInput(response, lines, false, new HashMap<>());
    }
    
    public void openSignInput(Consumer<SignInputContext> response, Component[] lines, boolean forwardInitialData) {
        openSignInput(response, lines, forwardInitialData, new HashMap<>());
    }
    
    public void openSignInput(Consumer<SignInputContext> response, Component[] lines, boolean forwardInitialData, @NotNull Map<String, Object> initialData) {
        initialData.put("response", SignInputMenu.Response.builder().handler(response).build());
        initialData.put("lines", lines);
        
        if (forwardInitialData) {
            initialData.putAll(rawData);
        }
        
        this.onSignInput = true;
        openMenu(SignInputMenu.class, false, initialData);
    }

    public int[] getBorders() {
        int size = menuConfig.getRows() * 9;
        return IntStream.range(0, size).filter(i -> size < 27 || i < 9
                                                    || i % 9 == 0 || (i - 8) % 9 == 0 || i > size - 9).toArray();
    }

    public int[] getCorners() {
        int size = menuConfig.getRows() * 9;
        return IntStream.range(0, size)
            .filter(i -> i < 2 || (i > 6 && i < 10)
                         || i == 17 || i == size - 18
                         || (i > size - 11 && i < size - 7) || i > size - 3).toArray();
    }

    @NotNull
    public MenuConfig getMenuConfig() {
        return menuConfig;
    }

    @SuppressWarnings("deprecation")
    private Inventory createInv() {
        return switch (menuConfig.getTitle()) {
            case String title -> Bukkit.createInventory(this, menuConfig.getRows() * 9, title);
            case Component title -> Bukkit.createInventory(this, menuConfig.getRows() * 9, title);
            default ->
                throw new IllegalArgumentException("Unsupported title type: " + menuConfig.getTitle().getClass().getName());
        };
    }

    /**
     * Atalho para usar o player do click diretamente.
     *
     * @param playerClickHandler o manipulador de clique do jogador
     * @return um Consumer<InventoryClickEvent> que chama o manipulador de clique do jogador
     */
    protected Consumer<InventoryClickEvent> playerClickHandler(@NotNull Consumer<Player> playerClickHandler) {
        return event -> {
            if (event.getWhoClicked() instanceof Player clicker) {
                playerClickHandler.accept(clicker);
            }
        };
    }
}
