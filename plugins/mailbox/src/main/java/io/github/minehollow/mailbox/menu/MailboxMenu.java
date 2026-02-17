package io.github.minehollow.mailbox.menu;

import io.github.minehollow.mailbox.MailboxPlugin;
import io.github.minehollow.mailbox.model.MailboxItem;
import io.github.minehollow.mailbox.model.PlayerMailboxData;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenuManager;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.menu.pagination.MenuPaginationContext;
import io.github.minehollow.minecraft.menu.pagination.SimplePaginationMenu;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@RequiredArgsConstructor
public class MailboxMenu extends SimplePaginationMenu<MailboxItem> {
    private static final PredefinedSound CLAIM_SOUND = new PredefinedSound(Sound.ENTITY_PLAYER_LEVELUP, 0.5F, 1.0F);
    private static final PredefinedSound CLAIM_ALL_SOUND = new PredefinedSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F,
            1.0F);
    private static final PredefinedSound ERROR_SOUND = new PredefinedSound(Sound.ENTITY_VILLAGER_NO, 0.5F, 1.0F);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final MailboxPlugin plugin;

    @Override
    public boolean onPreOpen(@NotNull MenuPaginationContext<MailboxItem> ctx, @NotNull MenuConfig openHandler) {
        final Player player = ctx.getPlayer();
        final var service = plugin.getMailboxService();
        final var data = service.getCachedData(player.getUniqueId());

        if (data == null) {
            StringUtils.send(player, "<red>Erro ao carregar dados. Tente novamente.");
            return false;
        }

        openHandler.setTitle(StringUtils.text("Correio"));
        openHandler.setLayout(
                "    H    ",
                " BBBBBBB ",
                " BBBBBBB ",
                " BBBBBBB ",
                "P       N",
                "    C    ");

        openHandler.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5F, 1.0F));

        ctx.setItem('H', ItemBuilder.skull(player)
                .name("<gradient:#FFD700:#FFA500><bold>CORREIO</bold></gradient>")
                .addLore(
                        " <dark_gray>Suas entregas pendentes.",
                        "",
                        " <white>Pendentes: <green><bold>" + data.getPendingCount() + "</bold>",
                        "",
                        " <gray>Clique esquerdo <dark_gray>= Pré-visualizar",
                        " <gray>Clique direito <dark_gray>= Resgatar direto")
                .build());

        ctx.setItem('C', ItemBuilder.of(Material.HOPPER)
                .name("<green><bold>RESGATAR TUDO</bold>")
                .addLore(
                        "",
                        " <gray>Resgata todas as entregas pendentes.",
                        " <gray>Itens sem espaço cairão no chão.",
                        "",
                        " <yellow>Clique para resgatar!")
                .build(),
                event -> handleClaimAll(ctx, player));

        return true;
    }

    @Override
    protected void openToPlayer(@NotNull SimpleMenuManager manager, @NotNull Player player,
            @NotNull Map<String, Object> initialData) {
        final var openHandler = new MenuConfig(defaultConfig);
        final MenuPaginationContext<MailboxItem> ctx = new MenuPaginationContext<>(player, initialData, openHandler,
                this, manager);
        if (!this.onPreOpen(ctx, openHandler))
            return;

        ctx.initialize();

        // setPagination must be called AFTER initialize() because it triggers update()
        var data = plugin.getMailboxService().getCachedData(player.getUniqueId());
        if (data != null) {
            ctx.setPagination('B', data.getBoxes(),
                    (p, box) -> renderBoxIcon(box),
                    (box, event) -> {
                        Player clicker = (Player) event.getWhoClicked();
                        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                            handleClaimBox(ctx, clicker, box);
                        } else {
                            ctx.openMenu(BoxPreviewMenu.class, Map.of("boxId", box.getId()));
                        }
                    });

            ctx.setPreviousButton('P', ItemBuilder.of(Material.ARROW)
                    .name("<yellow>Página Anterior")
                    .build());

            ctx.setNextButton('N', ItemBuilder.of(Material.ARROW)
                    .name("<yellow>Próxima Página")
                    .build());
        }

        playerContexts.put(player.getUniqueId(), ctx);
        ctx.open();
    }

    @Override
    protected void onClose(@NotNull PlayerMenuContext ctx, @NotNull InventoryCloseEvent event) {
        var playerId = ctx.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            Player p = plugin.getServer().getPlayer(playerId);
            if (p == null || !p.isOnline()) {
                plugin.getMailboxService().unload(playerId);
            }
        }, 60L);
    }

    private @NotNull ItemStack renderBoxIcon(@NotNull MailboxItem box) {
        Material icon;
        try {
            icon = Material.valueOf(box.getIcon().toUpperCase());
        } catch (IllegalArgumentException e) {
            icon = Material.CHEST;
        }

        String date = DATE_FORMAT.format(new Date(box.getSentAt()));

        return ItemBuilder.of(icon)
                .name("<white>" + box.getDescription())
                .addLore(
                        "",
                        " <gray>De: <white>" + box.getSenderPlugin(),
                        " <gray>Data: <white>" + date,
                        " <gray>Itens: <white>" + box.getItemCount(),
                        "",
                        " <green>Clique esquerdo <dark_gray>= Pré-visualizar",
                        " <red>Clique direito <dark_gray>= Resgatar direto")
                .build();
    }

    private void handleClaimBox(@NotNull MenuPaginationContext<MailboxItem> ctx, @NotNull Player player,
            @NotNull MailboxItem box) {
        boolean success = plugin.getMailboxService().claimBox(player, box.getId());
        if (success) {
            StringUtils.send(player, "<green><bold>RESGATADO!</bold> <gray>" + box.getDescription());
            CLAIM_SOUND.play(player);
            ctx.openMenu(MailboxMenu.class);
        } else {
            StringUtils.send(player, "<red>Erro ao resgatar esta entrega.");
            ERROR_SOUND.play(player);
        }
    }

    private void handleClaimAll(@NotNull MenuPaginationContext<MailboxItem> ctx, @NotNull Player player) {
        int claimed = plugin.getMailboxService().claimAll(player);
        if (claimed > 0) {
            StringUtils.send(player,
                    "<green><bold>SUCESSO!</bold> <gray>Resgatou <white>" + claimed + " <gray>entrega(s).");
            CLAIM_ALL_SOUND.play(player);
            ctx.openMenu(MailboxMenu.class);
        } else {
            StringUtils.send(player, "<red>Nenhuma entrega para resgatar.");
            ERROR_SOUND.play(player);
        }
    }
}
