package io.github.minehollow.clans.menu;

import io.github.minehollow.clans.ClansPlugin;
import io.github.minehollow.clans.config.MessageConfig;
import io.github.minehollow.clans.model.Clan;
import io.github.minehollow.clans.model.ClanMember;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Menu that lists clan members so the owner can pick one to transfer leadership to.
 * Clicking a member opens the {@link ClanConfirmationMenu} with the transfer action.
 */
@RequiredArgsConstructor
public class ClanTransferSelectMenu extends SimpleMenu {

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
            player.sendMessage(msg().get("menu.owner-only-transfer"));
            return false;
        }

        openHandler.setTitle(StringUtils.text("<gold>Selecionar Novo Líder"));
        openHandler.setRows(4);

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int idx = 0;

        for (ClanMember member : clan.getMembers()) {
            // Skip owner (can't transfer to yourself)
            if (member.getUuid().equals(player.getUniqueId())) continue;
            if (idx >= slots.length) break;

            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            String memberName = memberPlayer != null ? memberPlayer.getName()
                : Bukkit.getOfflinePlayer(member.getUuid()).getName();
            if (memberName == null) memberName = "Desconhecido";

            int slot = slots[idx++];
            final String finalMemberName = memberName;

            ctx.setItem(slot, ItemBuilder.of(memberPlayer != null ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("<white><bold>" + finalMemberName + "</bold></white>")
                .addLore(
                    " ",
                    " <gray>Status:</gray> " + (memberPlayer != null ? "<green>Online" : "<gray>Offline"),
                    " ",
                    " <yellow>Clique para transferir</yellow>",
                    " <yellow>a liderança para este jogador</yellow>",
                    " "
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build(), e -> {
                    player.closeInventory();
                    Map<String, Object> data = new HashMap<>();
                    data.put(ClanConfirmationMenu.KEY_ACTION, ClanConfirmationMenu.ACTION_TRANSFER);
                    data.put(ClanConfirmationMenu.KEY_TRANSFER_TARGET, member.getUuid());
                    MenuUtil.openMenu(player, ClanConfirmationMenu.class, data);
                }
            );
        }

        if (idx == 0) {
            ctx.setItem(13, ItemBuilder.of(Material.BARRIER)
                .name("<red>Sem membros disponíveis</red>")
                .addLore(
                    " ",
                    " <gray>Não há outros membros no clan</gray>",
                    " <gray>para transferir a liderança.</gray>",
                    " "
                )
                .build()
            );
        }

        ctx.setItem(31, ItemBuilder.of(Material.ARROW)
            .name("<yellow>← Voltar</yellow>")
            .build(), e -> {
                player.closeInventory();
                MenuUtil.openMenu(player, ClanSettingsMenu.class);
            }
        );

        return true;
    }
}

