package io.github.minehollow.clans.menu;

import io.github.minehollow.clans.ClansPlugin;
import io.github.minehollow.clans.config.MessageConfig;
import io.github.minehollow.clans.model.Clan;
import io.github.minehollow.clans.service.ClanService;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.menu.PlayerMenuContext;
import io.github.minehollow.minecraft.menu.SimpleMenu;
import io.github.minehollow.minecraft.menu.config.MenuConfig;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Settings menu for clan owner - manage invites, transfer ownership, etc.
 */
@RequiredArgsConstructor
public class ClanSettingsMenu extends SimpleMenu {

    private final ClansPlugin plugin;

    private MessageConfig msg() {
        return plugin.getMessageConfig();
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        Player player = ctx.getPlayer();
        ClanService service = plugin.getClanService();

        Clan clan = service.getByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(msg().get("menu.not-in-clan"));
            return false;
        }

        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(msg().get("menu.owner-only-settings"));
            return false;
        }

        openHandler.setTitle(StringUtils.text("<aqua>Configurações - " + clan.getTag()));
        openHandler.setRows(4);
        openHandler.setLayout(
            "         ",
            " I T     ",
            "         ",
            "    B    "
        );

        // ── Pending Invites ──
        List<UUID> invites = clan.getPendingInvites();
        List<String> inviteLore = new ArrayList<>();
        inviteLore.add(" ");
        inviteLore.add(" <gray>Convites pendentes:</gray> <white>" + invites.size() + "</white>");
        inviteLore.add(" ");

        if (invites.isEmpty()) {
            inviteLore.add(" <gray>Nenhum convite pendente</gray>");
        } else {
            for (int i = 0; i < Math.min(invites.size(), 5); i++) {
                String name = Bukkit.getOfflinePlayer(invites.get(i)).getName();
                if (name == null) name = "Desconhecido";
                inviteLore.add(" <white>• " + name + "</white>");
            }
            if (invites.size() > 5) {
                inviteLore.add(" <gray>... e mais " + (invites.size() - 5) + "</gray>");
            }
        }
        inviteLore.add(" ");

        ctx.setItem('I', ItemBuilder.of(Material.PAPER)
            .name("<yellow><bold>📜 Convites Pendentes</bold></yellow>")
            .addLore(inviteLore.toArray(new String[0]))
            .build()
        );

        // ── Transfer Ownership ──
        // Build lore showing eligible members
        List<String> transferLore = new ArrayList<>();
        transferLore.add(" ");
        transferLore.add(" <gray>Transfira a liderança do clan</gray>");
        transferLore.add(" <gray>para outro membro</gray>");
        transferLore.add(" ");
        transferLore.add(" <dark_red>⚠ Esta ação é irreversível!</dark_red>");
        transferLore.add(" ");
        transferLore.add(" <yellow>Clique para selecionar o novo líder</yellow>");
        transferLore.add(" ");

        ctx.setItem('T', ItemBuilder.of(Material.GOLDEN_HELMET)
            .name("<gold><bold>👑 Transferir Liderança</bold></gold>")
            .addLore(transferLore.toArray(new String[0]))
            .flags(ItemFlag.HIDE_ATTRIBUTES)
            .build(), e -> {
                player.closeInventory();
                var data = new java.util.HashMap<String, Object>();
                data.put("transfer_mode", true);
                MenuUtil.openMenu(player, ClanTransferSelectMenu.class, data);
            }
        );

        // ── Back ──
        ctx.setItem('B', ItemBuilder.of(Material.ARROW)
            .name("<yellow>← Voltar</yellow>")
            .build(), e -> {
                ctx.getPlayer().closeInventory();
                MenuUtil.openMenu(player, ClanMainMenu.class);
            }
        );

        return true;
    }
}

