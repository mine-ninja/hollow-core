package io.github.minehollow.bestiary.menu;

import io.github.minehollow.bestiary.BestiaryPlugin;
import io.github.minehollow.bestiary.archetype.MobArchetype;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArchetypeListMenu extends SimpleMenu {

    private final BestiaryPlugin plugin;
    private static final int ITEMS_PER_PAGE = 45;

    public ArchetypeListMenu(BestiaryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        int page = ctx.getOrDefault("page", 0);
        List<MobArchetype> archetypes = new ArrayList<>(plugin.getMobArchetypeService().getAllCached());

        config.setTitle(StringUtils.text("Arquétipos de Monstros - Pag " + (page + 1)));
        config.setRows(6);
        config.setLayout(
            "AAAAAAAAA",
            "AAAAAAAAA",
            "AAAAAAAAA",
            "AAAAAAAAA",
            "AAAAAAAAA",
            "---P-N---"
        );

        config.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));

        int totalPages = (int) Math.ceil((double) archetypes.size() / ITEMS_PER_PAGE);
        int start = page * ITEMS_PER_PAGE;

        // Botões de navegação
        ctx.setItem('P', p -> createArrow(page > 0, "Anterior"), e -> {
            if (page > 0) {
                ctx.put("page", page - 1);
                ctx.openMenu(ArchetypeListMenu.class, true);
            }
        });

        ctx.setItem('N', p -> createArrow(page < totalPages - 1, "Próxima"), e -> {
            if (page < totalPages - 1) {
                ctx.put("page", page + 1);
                ctx.openMenu(ArchetypeListMenu.class, true);
            }
        });

        // Listagem dos Arquétipos
        int[] slots = config.getLayout().get('A');
        for (int i = 0; i < slots.length && (start + i) < archetypes.size(); i++) {
            MobArchetype archetype = archetypes.get(start + i);
            ctx.setItem(slots[i], createArchetypeItem(archetype));
            ctx.put("slot_" + slots[i], archetype);
        }

        return true;
    }

    @Override
    protected void onClick(@NotNull PlayerMenuContext ctx, @NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        MobArchetype archetype = ctx.get("slot_" + event.getRawSlot());
        if (archetype != null) {
            Tasks.runSync(() -> ctx.openMenu(ArchetypeEditMenu.class, Map.of("archetype", archetype)));
        }
    }

    private ItemStack createArchetypeItem(MobArchetype archetype) {
        return ItemBuilder.of(Material.ZOMBIE_HEAD)
            .name("<green>" + archetype.displayName())
            .lore(
                "<gray>ID: <white>" + archetype.id(),
                "<gray>Entidade: <white>" + archetype.entityType(),
                "<gray>Níveis: <yellow>" + archetype.levelRange(),
                "",
                "<dark_gray>Clique para editar"
            ).build();
    }

    private ItemStack createArrow(boolean active, String name) {
        return ItemBuilder.of(active ? Material.ARROW : Material.GRAY_DYE)
            .name((active ? "<yellow>" : "<gray>") + name).build();
    }
}
