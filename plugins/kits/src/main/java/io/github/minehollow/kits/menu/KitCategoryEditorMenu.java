package io.github.minehollow.kits.menu;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.model.KitCategory;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class KitCategoryEditorMenu extends SimpleMenu {
    private static final ItemStack SAVE_BUTTON = ItemBuilder.of(Material.LIME_DYE)
            .name("<green>Salvar Categoria")
            .lore("", "<gray>Clique para salvar", "<gray>todas as alterações.")
            .build();

    private static final ItemStack DELETE_BUTTON = ItemBuilder.of(Material.RED_DYE)
            .name("<red>Deletar Categoria")
            .lore("", "<gray>Clique para deletar", "<gray>esta categoria.", "",
                    "<dark_red>Esta ação é irreversível!")
            .build();

    private static final ItemStack SEPARATOR = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();

    private final KitService kitService;

    public KitCategoryEditorMenu(KitService kitService) {
        this.kitService = kitService;
        this.defaultConfig.setGlobalClickCancelled(false);
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        Player player = ctx.getPlayer();

        if (!player.hasPermission("kit.admin")) {
            player.sendMessage(StringUtils.text("<red>Você não tem permissão para editar categorias!"));
            return false;
        }

        KitCategory category = ctx.get("category");
        boolean isNew = category == null;

        if (isNew) {
            String id = ctx.get("categoryId");
            String displayName = ctx.get("displayName");
            int priority = ctx.getOrDefault("priority", 0);

            if (id == null) {
                player.sendMessage(StringUtils.text("<red>ID da categoria não especificado!"));
                return false;
            }

            category = new KitCategory(id, displayName != null ? displayName : id, Material.CHEST);
            category.setPriority(priority);
            ctx.put("category", category);
        }

        final KitCategory currentCategory = category;

        config.setTitle(StringUtils.text(isNew
                ? "Criando Categoria: " + currentCategory.getId()
                : "Editando: " + currentCategory.getId()));
        config.setLayout(
                "---------",
                "-N-O-P-S-",
                "-------D-");
        config.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));

        // SEPARATOR
        ctx.setItem('-', SEPARATOR, e -> e.setCancelled(true));

        // NAME
        ctx.setItem('N', p -> createNameButton(currentCategory), e -> {
            e.setCancelled(true);
            openNameInput(ctx);
        });

        // ICON
        ctx.setItem('O', p -> createIconButton(currentCategory), e -> {
            ItemStack cursor = e.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR) {
                currentCategory.setIcon(cursor.getType());
                player.sendMessage(
                        StringUtils.text("<green>Ícone da categoria alterado para: <white>" + cursor.getType().name()));
                int[] iconSlots = ctx.getMenuConfig().getLayout().get('O');
                if (iconSlots != null && iconSlots.length > 0) {
                    ctx.getInventory().setItem(iconSlots[0], createIconButton(currentCategory));
                }
            } else {
                player.sendMessage(
                        StringUtils.text("<gray>Segure um item e clique aqui para alterar o ícone."));
            }
            e.setCancelled(true);
        });

        // PRIORITY
        ctx.setItem('P', p -> createPriorityButton(currentCategory), e -> {
            e.setCancelled(true);
            openPriorityInput(ctx);
        });

        // SAVE
        ctx.setItem('S', SAVE_BUTTON, e -> {
            e.setCancelled(true);
            saveCategory(ctx);
        });

        if (!isNew) {
            ctx.setItem('D', DELETE_BUTTON, e -> {
                e.setCancelled(true);
                deleteCategory(ctx, currentCategory);
            });
        }

        return true;
    }

    @Override
    protected void onClick(@NotNull PlayerMenuContext ctx, @NotNull InventoryClickEvent event) {
        // Allow player inventory clicks (bottom slots) for picking up items
        if (event.getRawSlot() >= ctx.getInventory().getSize()) {
            return;
        }
        event.setCancelled(true);
    }

    private void openNameInput(PlayerMenuContext ctx) {
        KitCategory category = ctx.get("category");
        if (category == null)
            return;

        ctx.openSignInput(signCtx -> {
            KitCategory cat = signCtx.get("category");
            String[] lines = signCtx.get("input");
            String input = lines != null && lines.length > 0 ? lines[0] : null;
            if (cat != null && input != null && !input.isBlank()) {
                cat.setDisplayName(input.trim());
                signCtx.getPlayer()
                        .sendMessage(StringUtils.text("<green>Nome alterado para: <white>" + cat.getDisplayName()));
            }
            signCtx.openMenu(KitCategoryEditorMenu.class, true);
        }, new Component[] {
                Component.empty(),
                Component.text("^^^^^^^^^^^^^^^"),
                Component.text("Digite o nome"),
                Component.text("da categoria")
        }, true);
    }

    private void openPriorityInput(PlayerMenuContext ctx) {
        KitCategory category = ctx.get("category");
        if (category == null)
            return;

        ctx.openSignInput(signCtx -> {
            KitCategory cat = signCtx.get("category");
            String[] lines = signCtx.get("input");
            String input = lines != null && lines.length > 0 ? lines[0] : null;
            if (cat != null && input != null && !input.isBlank()) {
                try {
                    int priority = Integer.parseInt(input.trim());
                    cat.setPriority(priority);
                    signCtx.getPlayer()
                            .sendMessage(StringUtils.text("<green>Prioridade alterada para: <yellow>" + priority));
                } catch (NumberFormatException e) {
                    signCtx.getPlayer().sendMessage(StringUtils.text("<red>Número inválido!"));
                }
            }
            signCtx.openMenu(KitCategoryEditorMenu.class, true);
        }, new Component[] {
                null,
                Component.text("^^^^^^^^^^^^^^^"),
                Component.text("Digite a prioridade"),
                Component.text("(número inteiro)")
        }, true);
    }

    private void saveCategory(PlayerMenuContext ctx) {
        KitCategory category = ctx.get("category");
        if (category == null)
            return;

        Player player = ctx.getPlayer();

        kitService.saveCategory(category).thenAccept(saved -> Tasks.runSync(() -> {
            player.closeInventory();
            player.sendMessage(
                    StringUtils.text(
                            "<green>Categoria <white>" + saved.getDisplayName() + " <green>salva com sucesso!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        })).exceptionally(ex -> {
            Tasks.runSync(
                    () -> player.sendMessage(StringUtils.text("<red>Erro ao salvar categoria: " + ex.getMessage())));
            return null;
        });
    }

    private void deleteCategory(PlayerMenuContext ctx, KitCategory category) {
        Player player = ctx.getPlayer();

        int kitCount = kitService.getKitsByCategory(category.getId()).size();
        if (kitCount > 0) {
            player.sendMessage(StringUtils.text(
                    "<red>Existem " + kitCount + " kits nesta categoria! Mova ou delete-os primeiro."));
            return;
        }

        kitService.deleteCategory(category.getId()).thenAccept(v -> Tasks.runSync(() -> {
            player.closeInventory();
            player.sendMessage(
                    StringUtils.text("<green>Categoria <white>" + category.getDisplayName() + " <green>deletada!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1f, 1f);
        })).exceptionally(ex -> {
            Tasks.runSync(() -> player.sendMessage(StringUtils.text("<red>Erro ao deletar: " + ex.getMessage())));
            return null;
        });
    }

    private ItemStack createNameButton(KitCategory category) {
        return ItemBuilder.of(Material.NAME_TAG)
                .name("<white><bold>Editar Nome")
                .lore("<gray>Nome atual: <white>" + category.getDisplayName(), "", "<dark_gray>Clique para editar")
                .build();
    }

    private ItemStack createIconButton(KitCategory category) {
        Material icon = category.getIcon() != null ? category.getIcon() : Material.CHEST;
        return ItemBuilder.of(icon)
                .name("<white><bold>Alterar Ícone")
                .lore("<gray>Ícone atual: <white>" + icon.name(), "", "<dark_gray>Segure um item e clique aqui")
                .build();
    }

    private ItemStack createPriorityButton(KitCategory category) {
        return ItemBuilder.of(Material.HOPPER)
                .name("<white><bold>Editar Prioridade")
                .lore("<gray>Prioridade atual: <yellow>" + category.getPriority(), "",
                        "<dark_gray>Menor = aparece primeiro", "", "<dark_gray>Clique para editar")
                .build();
    }
}
