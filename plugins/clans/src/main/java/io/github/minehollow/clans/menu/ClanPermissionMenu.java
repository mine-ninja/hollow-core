package io.github.minehollow.clans.menu;

import io.github.minehollow.clans.ClansPlugin;
import io.github.minehollow.clans.config.MessageConfig;
import io.github.minehollow.clans.model.Clan;
import io.github.minehollow.clans.model.ClanMember;
import io.github.minehollow.clans.model.ClanPermission;
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
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Menu for managing a specific member's permissions.
 */
@RequiredArgsConstructor
public class ClanPermissionMenu extends SimpleMenu {

    private final ClansPlugin plugin;

    private MessageConfig msg() {
        return plugin.getMessageConfig();
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        Player player = ctx.getPlayer();
        UUID targetUuid = ctx.get("target_member");
        if (targetUuid == null) {
            player.sendMessage(msg().get("menu.member-not-found"));
            return false;
        }

        ClanService service = plugin.getClanService();
        Clan clan = service.getByPlayer(player.getUniqueId());
        if (clan == null || !clan.isOwner(player.getUniqueId())) {
            player.sendMessage(msg().get("menu.owner-only-permissions"));
            return false;
        }

        ClanMember target = clan.getMember(targetUuid);
        if (target == null) {
            player.sendMessage(msg().get("menu.member-not-found"));
            return false;
        }

        String targetName = Bukkit.getOfflinePlayer(targetUuid).getName();
        if (targetName == null) targetName = "Desconhecido";

        openHandler.setTitle(StringUtils.text("<aqua>Permissões - " + targetName));
        openHandler.setRows(3);

        ClanPermission[] perms = ClanPermission.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16};

        for (int i = 0; i < perms.length && i < slots.length; i++) {
            ClanPermission perm = perms[i];
            int slot = slots[i];
            boolean hasPerm = target.hasPermission(perm);

            ItemBuilder builder = ItemBuilder.of(hasPerm ? Material.LIME_DYE : Material.GRAY_DYE)
                .name((hasPerm ? "<green>" : "<gray>") + perm.getDisplayName())
                .addLore(
                    " ",
                    " <gray>" + perm.getDescription() + "</gray>",
                    " ",
                    " <gray>Status:</gray> " + (hasPerm ? "<green>Ativado" : "<red>Desativado"),
                    " ",
                    " <yellow>Clique para alternar</yellow>",
                    " "
                );

            if (hasPerm) builder.glow();

            ctx.setItem(slot, builder.build(), e -> {
                    Thread.startVirtualThread(() -> {
                        var result = hasPerm ?
                            service.revokePermission(player.getUniqueId(), targetUuid, perm) :
                            service.grantPermission(player.getUniqueId(), targetUuid, perm);

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (result == io.github.minehollow.clans.service.ClanResult.SUCCESS) {
                                ctx.update();
                            } else {
                                player.sendMessage(result.getMessage());
                            }
                        });
                    });
                }
            );
        }

        // Back button
        ctx.setItem(22, ItemBuilder.of(Material.ARROW)
            .name("<yellow>← Voltar</yellow>")
            .build(), e -> {
                ctx.getPlayer().closeInventory();
                MenuUtil.openMenu(player, ClanMembersMenu.class);
            }
        );

        return true;
    }
}

