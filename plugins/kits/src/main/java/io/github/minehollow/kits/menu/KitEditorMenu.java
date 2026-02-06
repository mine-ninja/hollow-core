package io.github.minehollow.kits.menu;

import io.github.minehollow.kits.KitService;
import io.github.minehollow.kits.model.Kit;
import io.github.minehollow.kits.util.MenuItemsUtil;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.task.Tasks;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.message.input.ChatInput;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import io.github.minehollow.sdk.util.time.Time;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class KitEditorMenu extends SimpleMenu {
    private final KitService kitService;

    public KitEditorMenu(KitService kitService) {
        this.kitService = kitService;
        this.defaultConfig.setGlobalClickCancelled(false);
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        Player player = ctx.getPlayer();

        if (!player.hasPermission("kit.admin")) {
            player.sendMessage(StringUtils.text("<red>Você não tem permissão para editar kits!"));
            return false;
        }

        Kit kit = ctx.get("kit");
        boolean isNew = kit == null;

        if (isNew) {
            String kitId = ctx.get("kitId");
            if (kitId == null) {
                player.sendMessage(StringUtils.text("<red>ID do kit não especificado!"));
                return false;
            }
            kit = new Kit();
            kit.setId(kitId);
            kit.setDisplayName(kitId);
            kit.setCooldown(0);
            kit.setIcon(Material.CHEST);
            kit.setItems(List.of());
            ctx.put("kit", kit);
        }

        final Kit currentKit = kit;

        config.setTitle(StringUtils.text(isNew
                ? "Criando Kit: " + currentKit.getDisplayName()
                : "Editando Kit: " + currentKit.getDisplayName()));
        config.setLayout(
                "IIIIIIIII",
                "IIIIIIIII",
                "IIIIIIIII",
                "IIIIIIIII",
                "---------",
                "-NC-O-SD-");
        config.setGlobalClickCancelled(false);
        config.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5f, 1f));

        // SEPARATOR
        ctx.setItem('-', MenuItemsUtil.SEPARATOR, e -> e.setCancelled(true));

        // NAME
        ctx.setItem('N', p -> createNameButton(currentKit), e -> {
            e.setCancelled(true);
            saveItemsFromInventory(ctx, currentKit);
            openNameInput(ctx);
        });

        // COOLDOWN
        ctx.setItem('C', p -> createCooldownButton(currentKit), e -> {
            e.setCancelled(true);
            saveItemsFromInventory(ctx, currentKit);
            openCooldownInput(ctx);
        });

        // ICON
        ctx.setItem('O', p -> createIconButton(currentKit), e -> {
            e.setCancelled(true);
            ItemStack cursor = e.getCursor();
            if (cursor.getType() != Material.AIR) {
                currentKit.setIcon(cursor.getType());
                player.sendMessage(
                        StringUtils.text("<green>Ícone do kit alterado para: <white>" + cursor.getType().name()));
                int[] iconSlots = ctx.getMenuConfig().getLayout().get('O');
                if (iconSlots != null && iconSlots.length > 0) {
                    ctx.getInventory().setItem(iconSlots[0], createIconButton(currentKit));
                }
            } else {
                player.sendMessage(
                        StringUtils.text("<gray>Coloque um item no cursor e clique aqui para alterar o ícone."));
            }
        });

        // SAVE
        ctx.setItem('S', MenuItemsUtil.SAVE_BUTTON, e -> {
            e.setCancelled(true);
            saveKit(ctx);
        });

        // DELETE
        if (!isNew) {
            ctx.setItem('D', MenuItemsUtil.DELETE_BUTTON, e -> {
                e.setCancelled(true);
                deleteKit(ctx, currentKit);
            });
        }

        return true;
    }

    @Override
    public void onPostOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig config) {
        Kit kit = ctx.get("kit");
        if (kit != null) {
            loadKitItems(ctx, kit);
        }
    }

    @Override
    protected void onClick(@NotNull PlayerMenuContext ctx, @NotNull InventoryClickEvent event) {
        int slot = event.getRawSlot();
        int[] itemSlots = ctx.getMenuConfig().getLayout().get('I');

        boolean isItemSlot = false;
        for (int s : itemSlots) {
            if (slot == s) {
                isItemSlot = true;
                break;
            }
        }

        if (isItemSlot || slot >= ctx.getInventory().getSize()) {
            return;
        }

        event.setCancelled(true);
    }

    private void loadKitItems(PlayerMenuContext ctx, Kit kit) {
        List<ItemStack> items = kit.getItems();
        int[] slots = ctx.getMenuConfig().getLayout().get('I');
        for (int i = 0; i < slots.length && i < items.size(); i++) {
            if (items.get(i) != null) {
                ctx.getInventory().setItem(slots[i], items.get(i).clone());
            }
        }
    }

    private void saveItemsFromInventory(PlayerMenuContext ctx, Kit kit) {
        int[] slots = ctx.getMenuConfig().getLayout().get('I');
        List<ItemStack> items = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = ctx.getInventory().getItem(slot);
            if (item != null && item.getType() != Material.AIR)
                items.add(item.clone());
        }
        kit.setItems(items);
    }


    private void openNameInput(PlayerMenuContext ctx) {
        Kit kit = ctx.get("kit");
        if (kit == null) return;

        Player player = ctx.getPlayer();
        player.closeInventory();
        ChatInput.waitInput(player, input -> {
            if (!input.equalsIgnoreCase("cancelar") && !input.isBlank()) {
                kit.setDisplayName(input.trim());
                ctx.getPlayer().sendMessage(StringUtils.text("<green>Nome do kit alterado para: " + kit.getDisplayName()));
            } else {
                ctx.getPlayer().sendMessage(StringUtils.text("<red>Edição cancelada."));
            }
            Tasks.runSync(() -> ctx.openMenu(KitEditorMenu.class, true));
        }, 30000L, "<green>Digite o novo nome do kit no chat.\n\n\n<gray>Digite <red>cancelar <gray>para voltar.");
    }

    private void openCooldownInput(PlayerMenuContext ctx) {
        Kit kit = ctx.get("kit");
        if (kit == null)
            return;

        Player player = ctx.getPlayer();
        player.closeInventory();
        ChatInput.waitInput(player, input -> {
                    if (!input.equalsIgnoreCase("cancelar") && !input.isBlank()) {
                        try {
                            Time time = Time.parseString(input.trim());
                            kit.setCooldown((long) time.toSeconds());
                            ctx.getPlayer().sendMessage(StringUtils.text("<green>Cooldown alterado para: <yellow>" + time));
                        } catch (Exception e) {
                            ctx.getPlayer().sendMessage(StringUtils.text("<red>Formato inválido! Use: 1h30m, 2d, 30s. Alteração cancelada."));
                        }
                    } else {
                        ctx.getPlayer().sendMessage(StringUtils.text("<red>Edição cancelada."));
                    }
                    Tasks.runSync(() -> ctx.openMenu(KitEditorMenu.class, true));
                }, 30000L,
                "<green>Digite o cooldown (Ex: 10m, 1h) no chat.\n\n\n<gray>Digite <red>cancelar <gray>para voltar.");
    }

    private void saveKit(PlayerMenuContext ctx) {
        Player player = ctx.getPlayer();
        Kit kit = ctx.get("kit");
        if (kit == null)
            return;

        saveItemsFromInventory(ctx, kit);

        kitService.saveKit(kit).thenAccept(saved -> Tasks.runSync(() -> {
            player.closeInventory();
            player.sendMessage(
                    StringUtils.text("<green>Kit <white>" + saved.getDisplayName() + " <green>salvo com sucesso!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        })).exceptionally(ex -> {
            player.sendMessage(StringUtils.text("<red>Erro ao salvar: " + ex.getMessage()));
            return null;
        });
    }

    private void deleteKit(PlayerMenuContext ctx, Kit kit) {
        Player player = ctx.getPlayer();
        kitService.deleteKit(kit.getId()).thenAccept(v -> Tasks.runSync(() -> {
            player.closeInventory();
            player.sendMessage(StringUtils.text("<green>Kit <white>" + kit.getDisplayName() + " <green>deletado!"));
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 1f);
        })).exceptionally(ex -> {
            player.sendMessage(StringUtils.text("<red>Erro ao deletar: " + ex.getMessage()));
            return null;
        });
    }

    private ItemStack createNameButton(Kit kit) {
        return ItemBuilder.of(Material.NAME_TAG)
                .name("<white>Editar Nome")
                .lore("<gray>Nome atual: <white>" + kit.getDisplayName(), "", "<dark_gray>Clique para editar")
                .build();
    }

    private ItemStack createCooldownButton(Kit kit) {
        String cooldown = kit.hasCooldown() ? new Time(kit.getCooldown(), TimeUnit.SECONDS).toString() : "Nenhum";
        return ItemBuilder.of(Material.CLOCK)
                .name("<white>Editar Cooldown")
                .lore("<gray>Cooldown atual: <yellow>" + cooldown, "", "<dark_gray>Clique para editar")
                .build();
    }

    private ItemStack createIconButton(Kit kit) {
        Material icon = kit.getIcon() != null ? kit.getIcon() : Material.CHEST;
        return ItemBuilder.of(icon)
                .name("<white>Alterar Ícone")
                .lore("<gray>Ícone atual: <white>" + icon.name(), "", "<dark_gray>Segure um item e clique aqui")
                .build();
    }
}
