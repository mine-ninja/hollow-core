package net.warcane.lugin.core.minecraft.menu.item;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Representa um item que será renderizado para um jogador em um menu (individualmente).
 */
@AllArgsConstructor @NoArgsConstructor
public final class SimpleMenuItem {
    private @NotNull Function<Player, ItemStack> renderer = player -> ItemStack.empty();
    private @NotNull Consumer<InventoryClickEvent> clickHandler = event -> event.setCancelled(true);
    
    /**
     * Cria um novo item de menu simples com um manipulador de clique vazio.
     *
     * @param renderer A função que renderiza o item para o jogador.
     */
    public SimpleMenuItem(@NotNull Function<Player, ItemStack> renderer) {
        this.renderer = renderer;
    }
    
    /**
     * Cria um novo item de menu simples com um manipulador de clique vazio.
     *
     * @param itemStack O ItemStack que será renderizado para o jogador.
     */
    public SimpleMenuItem(@NotNull ItemStack itemStack, @NotNull Consumer<InventoryClickEvent> clickHandler) {
        this.renderer = player -> itemStack;
        this.clickHandler = clickHandler;
    }
    
    /**
     * Cria um novo item de menu simples com um manipulador de clique vazio.
     *
     * @param itemStack O ItemStack que será renderizado para o jogador.
     */
    public SimpleMenuItem(@NotNull ItemStack itemStack) {
        this.renderer = player -> itemStack;
    }
    
    public SimpleMenuItem renderer(Function<Player, ItemStack> renderer) {
        this.renderer = renderer;
        return this;
    }
    
    public SimpleMenuItem clickHandler(Consumer<InventoryClickEvent> clickHandler) {
        this.clickHandler = clickHandler;
        return this;
    }
    
    public @NotNull Function<Player, ItemStack> renderer() { return renderer; }
    
    public @NotNull Consumer<InventoryClickEvent> clickHandler() { return clickHandler; }
}
