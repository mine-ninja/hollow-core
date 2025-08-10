package net.warcane.lugin.core.minecraft.menu.item;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Representa um item que será renderizado para um jogador em um menu (individualmente).
 */
public record SimpleMenuItem(
  @NotNull Function<Player, ItemStack> renderer,
  @NotNull Consumer<InventoryClickEvent> clickHandler
) {

    /**
     * Cria um novo item de menu simples com um manipulador de clique vazio.
     *
     * @param renderer A função que renderiza o item para o jogador.
     */
    public SimpleMenuItem(@NotNull Function<Player, ItemStack> renderer) {
        this(renderer, event -> event.setCancelled(true));
    }

    /**
     * Cria um novo item de menu simples com um manipulador de clique vazio.
     *
     * @param itemStack O ItemStack que será renderizado para o jogador.
     */
    public SimpleMenuItem(@NotNull ItemStack itemStack, @NotNull Consumer<InventoryClickEvent> clickHandler) {
        this(player -> itemStack, clickHandler);
    }

    /**
     * Cria um novo item de menu simples com um manipulador de clique vazio.
     *
     * @param itemStack O ItemStack que será renderizado para o jogador.
     */
    public SimpleMenuItem(@NotNull ItemStack itemStack) {
        this(itemStack, event -> event.setCancelled(true));
    }
}
