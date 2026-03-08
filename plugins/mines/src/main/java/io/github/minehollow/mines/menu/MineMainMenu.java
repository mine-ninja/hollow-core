package io.github.minehollow.mines.menu;

import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.menu.pagination.DynamicPaginationContext;
import io.github.minehollow.minecraft.menu.pagination.DynamicPaginationMenu;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.mines.MinesPlugin;
import io.github.minehollow.mines.mine.MineDefinition;
import io.github.minehollow.mines.service.VirtualMineService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import io.github.minehollow.mines.util.SimpleCuboidArea;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class MineMainMenu extends DynamicPaginationMenu<MineDefinition> {

    private static final String DEFAULT_MINE_HEAD = "f8bc2f280ec0dcf9fcb8fa95bd35f40f0fd6782f123cbf2f4a3629f0652f6f38";

    private final MinesPlugin plugin;
    private final VirtualMineService mineService;

    @Override
    public boolean onPreOpen(@NotNull DynamicPaginationContext<MineDefinition> ctx, @NotNull MenuConfig openHandler) {
        openHandler.setTitle(StringUtils.text("Menu de Minas"));
        openHandler.setRows(6);
        openHandler.setLayout(
            "BBBBBBBBB",
            "BMMMMMMMB",
            "BMMMMMMMB",
            "BMMMMMMMB",
            "BMMMMMMMB",
            "BBBBBBBBB"
        );

        return true;
    }

    @Override
    public void onPostOpen(@NotNull DynamicPaginationContext<MineDefinition> ctx, @NotNull MenuConfig openHandler) {

        final ItemStack border = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
            .name("<dark_gray> ")
            .build();

        ctx.setItem('B', border);

        final List<MineDefinition> definitions = new ArrayList<>(plugin.getDefinitionRegistry().getAll());
        definitions.sort(Comparator.comparing(MineDefinition::getId, String.CASE_INSENSITIVE_ORDER));

        if (definitions.isEmpty()) {
            ctx.setItem(
                'M', ItemBuilder.of(Material.BARRIER)
                    .name("<red>Nenhuma mina encontrada")
                    .addLore(" <gray>Configure uma mina no config.yml")
                    .build()
            );
        } else {
            ctx.setPagination(
                'M', definitions, this::renderMineIcon, (definition, event) -> {
                    if (!(event.getWhoClicked() instanceof Player player)) {
                        return;
                    }

                    final boolean success = mineService.teleportToDefinition(player, definition);
                    if (success) {
                        player.sendMessage(MinesPlugin.messages().mm("mine.teleport", java.util.Map.of("id", definition.getId())));
                    } else {
                        player.sendMessage(MinesPlugin.messages().mm("mine.teleport_failed"));
                    }
                    player.closeInventory();
                }
            );
        }

        ctx.setPreviousButton(
            'P', ItemBuilder.of(Material.ARROW)
                .name("<yellow>Pagina anterior")
                .build()
        );
        ctx.setNextButton(
            'N', ItemBuilder.of(Material.ARROW)
                .name("<yellow>Proxima pagina")
                .build()
        );
    }

    private @NotNull ItemStack renderMineIcon(@NotNull Player player, @NotNull MineDefinition definition) {
        final ItemBuilder builder = this.resolveSkullBuilder(definition);

        final var miningArea = definition.getMiningArea();
        var sizeFormatted = "Desconecido";
        if(miningArea != null) {
            sizeFormatted = miningArea.getSizeFormatted();
        }

        return builder
            .name(definition.getDisplayName())
            .addLore(
                "",
                " <gray>Tamanho: <white>" + sizeFormatted,
                "",
                " <green>Clique para teleportar"
            )
            .glow()
            .build();
    }

    private @NotNull ItemBuilder resolveSkullBuilder(@NotNull MineDefinition definition) {
        final String headUrl = definition.getHeadUrl();

        if (headUrl != null && !headUrl.isBlank()) {
            try {
                return ItemBuilder.skull(headUrl.trim());
            } catch (Exception ignored) {
                // Fallback para head default quando a URL da mina for invalida.
            }
        }

        try {
            return ItemBuilder.skull(DEFAULT_MINE_HEAD);
        } catch (Exception ignored) {
            // Fallback final para nunca quebrar renderizacao de item.
            return ItemBuilder.of(Material.PLAYER_HEAD);
        }
    }
}
