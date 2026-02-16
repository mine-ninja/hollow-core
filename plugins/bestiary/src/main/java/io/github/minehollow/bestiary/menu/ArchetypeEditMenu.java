package io.github.minehollow.bestiary.menu;

import io.github.minehollow.bestiary.BestiaryPlugin;
import io.github.minehollow.bestiary.archetype.MobArchetype;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.message.input.ChatInput;
import io.github.minehollow.minecraft.util.range.DoubleRange;
import io.github.minehollow.minecraft.util.range.IntRange;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ArchetypeEditMenu extends SimpleMenu {

    private final BestiaryPlugin plugin;

    public ArchetypeEditMenu(BestiaryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        MobArchetype archetype = ctx.get("archetype");
        if (archetype == null) return false;

        config.setTitle(StringUtils.text("Editando: " + archetype.displayName()));
        config.setRows(3);
        config.setLayout(
            "---------",
            "-N-L-H-D-",
            "----S----"
        );

        ctx.setItem('-', p -> ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build(), e -> e.setCancelled(true));

        // Nome Exibido
        ctx.setItem('N', p -> ItemBuilder.of(Material.NAME_TAG)
            .name("<white>Nome: <yellow>" + archetype.displayName())
            .lore("<gray>Clique para alterar o nome.").build(), e -> {
            requestInput(ctx, "Digite o novo nome:", input -> {
                updateAndRefresh(ctx, archetype, input, null, null, null);
            });
        });

        // Range de Nível
        ctx.setItem('L', p -> ItemBuilder.of(Material.EXPERIENCE_BOTTLE)
            .name("<white>Nível: <yellow>" + archetype.levelRange())
            .lore("<gray>Ex: 1-10 ou 5").build(), e -> {
            requestInput(ctx, "Digite o range de nível (ex: 1-50):", input -> {
                updateAndRefresh(ctx, archetype, null, IntRange.parseString(input), null, null);
            });
        });

        // Vida por nível
        ctx.setItem('H', p -> ItemBuilder.of(Material.APPLE)
            .name("<white>Vida por Nível: <red>" + archetype.healthPerLevel())
            .lore("<gray>Ex: 10.5-20.0").build(), e -> {
            requestInput(ctx, "Digite o range de vida:", input -> {
                updateAndRefresh(ctx, archetype, null, null, DoubleRange.parseString(input), null);
            });
        });

        // Botão Salvar
        ctx.setItem('S', p -> ItemBuilder.of(Material.EMERALD)
            .name("<green>SALVAR ARQUÉTIPO")
            .lore("<gray>Salva as alterações no banco de dados.").build(), e -> {
            plugin.getMobArchetypeService().save(ctx.get("archetype"));
            ctx.getPlayer().sendMessage(StringUtils.text("<green>Arquétipo salvo com sucesso!"));
            ctx.getPlayer().playSound(ctx.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            ctx.openMenu(ArchetypeListMenu.class);
        });

        return true;
    }

    private void requestInput(PlayerMenuContext ctx, String msg, Consumer<String> action) {
        ctx.getPlayer().closeInventory();
        ChatInput.waitInput(ctx.getPlayer(), input -> {
            try {
                action.accept(input);
            } catch (Exception ex) {
                ctx.getPlayer().sendMessage(StringUtils.text("<red>Formato inválido!"));
                ctx.openMenu(ArchetypeEditMenu.class, true);
            }
        }, msg);
    }

    private void updateAndRefresh(PlayerMenuContext ctx, MobArchetype m, String name, IntRange lvl, DoubleRange hp, DoubleRange dmg) {
        MobArchetype updated = new MobArchetype(
            m.id(),
            name != null ? name : m.displayName(),
            m.entityType(),
            lvl != null ? lvl : m.levelRange(),
            hp != null ? hp : m.healthPerLevel(),
            dmg != null ? dmg : m.damagePerLevel(),
            m.equipment(),
            m.drops(),
            m.abilities()
        );
        ctx.put("archetype", updated);
        ctx.openMenu(ArchetypeEditMenu.class, true);
    }
}
