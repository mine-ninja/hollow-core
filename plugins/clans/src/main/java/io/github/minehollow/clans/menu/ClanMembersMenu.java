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
import io.github.minehollow.minecraft.util.Cooldown;
import io.github.minehollow.minecraft.util.item.ItemBuilder;
import io.github.minehollow.minecraft.util.message.StringUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Shows all clan members with management options.
 */
@RequiredArgsConstructor
public class ClanMembersMenu extends SimpleMenu {

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

        ClanMember viewer = clan.getMember(player.getUniqueId());
        boolean canManage = viewer != null && viewer.hasPermission(ClanPermission.MANAGE_MEMBERS);
        boolean isOwner = clan.isOwner(player.getUniqueId());

        openHandler.setTitle(StringUtils.text("<yellow>Membros - " + clan.getTag()));
        openHandler.setRows(4);

        List<ClanMember> members = new ArrayList<>(clan.getMembers());

        // Display members in slots 10-16, 19-25
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < members.size() && i < slots.length; i++) {
            ClanMember member = members.get(i);
            int slot = slots[i];

            boolean isMemberOwner = clan.isOwner(member.getUuid());
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            String memberName = memberPlayer != null ? memberPlayer.getName() :
                                Bukkit.getOfflinePlayer(member.getUuid()).getName();
            if (memberName == null) memberName = "Desconhecido";

            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(" <gray>Cargo:</gray> " + (isMemberOwner ? "<gold>Dono" : "<white>Membro"));
            lore.add(" <gray>Status:</gray> " + (memberPlayer != null ? "<green>Online" : "<gray>Offline"));
            lore.add(" ");
            lore.add(" <yellow>Permissões:</yellow>");

            for (ClanPermission perm : ClanPermission.values()) {
                boolean has = member.hasPermission(perm);
                lore.add(" " + (has ? "<green>✔" : "<red>✖") + " <gray>" + perm.getDisplayName());
            }

            if (canManage && !isMemberOwner && !member.getUuid().equals(player.getUniqueId())) {
                lore.add(" ");
                lore.add(" <red>Clique direito para expulsar</red>");
                lore.add(" <yellow>Clique esquerdo para gerenciar</yellow>");
            } else if (isOwner && !member.getUuid().equals(player.getUniqueId())) {
                lore.add(" ");
                lore.add(" <yellow>Clique para gerenciar permissões</yellow>");
            }

            lore.add(" ");

            // Use player head with correct skin
            ItemStack icon = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) icon.getItemMeta();
            if (skullMeta != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(member.getUuid());
                skullMeta.setOwningPlayer(offlinePlayer);
                icon.setItemMeta(skullMeta);
            }

            ctx.setItem(slot, ItemBuilder.of(icon)
                .name("<white><bold>" + memberName + "</bold></white>")
                .addLore(lore.toArray(new String[0]))
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build(), e -> {
                    if (e.isRightClick() && canManage && !isMemberOwner && !member.getUuid().equals(player.getUniqueId())) {
                        // Kick
                        player.closeInventory();
                        Thread.startVirtualThread(() -> {
                            var result = service.kick(player.getUniqueId(), member.getUuid());
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                player.sendMessage(result.getMessage());
                                if (result == io.github.minehollow.clans.service.ClanResult.SUCCESS) {
                                    Player target = Bukkit.getPlayer(member.getUuid());
                                    if (target != null) {
                                        target.sendMessage(msg().get("kick-target"));
                                    }
                                }
                            });
                        });
                    } else if (e.isLeftClick() && isOwner && !member.getUuid().equals(player.getUniqueId())) {
                        // Manage permissions
                        ctx.getPlayer().closeInventory();
                        var data = new HashMap<String, Object>();
                        data.put("target_member", member.getUuid());
                        MenuUtil.openMenu(player, ClanPermissionMenu.class, data);
                    }
                }
            );
        }

        // Back button
        ctx.setItem(31, ItemBuilder.of(Material.ARROW)
            .name("<yellow>← Voltar</yellow>")
            .build(), e -> {
                ctx.getPlayer().closeInventory();
                MenuUtil.openMenu(player, ClanMainMenu.class);
            }
        );

        return true;
    }
}
