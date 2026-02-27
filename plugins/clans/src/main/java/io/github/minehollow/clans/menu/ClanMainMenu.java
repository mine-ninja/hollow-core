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
import io.github.minehollow.minecraft.util.sound.PredefinedSound;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main clan menu showing clan info, members, and management options.
 */
@RequiredArgsConstructor
public class ClanMainMenu extends SimpleMenu {

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

        ClanMember member = clan.getMember(player.getUniqueId());
        boolean isOwner = clan.isOwner(player.getUniqueId());

        openHandler.setTitle(StringUtils.text("<black>Clan [" + clan.getTag() + "]"));
        openHandler.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK , 0.5f, 1.5f));
        openHandler.setRows(4);
        openHandler.setLayout(
            "         ",
            "    M    ",
            "  IFUSD  ",
            "         "
        );

        // ── Clan Info ──
        ctx.setItem(
            'I', ItemBuilder.of(Material.BOOK)
                .name("<gradient:#C77DFF:#9D4EDD><bold>✦ Informações do Clan</bold></gradient>")
                .addLore(
                    " ",
                    " <gray>Tag:</gray> <white><bold>" + clan.getTag() + "</bold></white>",
                    " <gray>Nome:</gray> <white>" + clan.getName() + "</white>",
                    " <gray>Dono:</gray> <white>" + resolvePlayerName(clan.getOwnerId()) + "</white>",
                    " <gray>Membros:</gray> <white>" + clan.getMembers().size() + "/" + clan.getMaxMembers(plugin.getSlotTable()) + "</white>",
                    " <gray>Nível de Vagas:</gray> <white>" + clan.getSlotTier() + "</white>",
                    " <gray>Fogo Amigo:</gray> " + (clan.isFriendlyFire() ? "<green>Ativado" : "<red>Desativado"),
                    " "
                )
                .flags(ItemFlag.HIDE_ATTRIBUTES)
                .build()
        );

        // ── Members ──
        ctx.setItem(
            'M', ItemBuilder.of(Material.PLAYER_HEAD)
                .name("<yellow><bold>👥 Membros do Clan</bold></yellow>")
                .addLore(
                    " ",
                    " <gray>Clique para ver todos os</gray>",
                    " <gray>membros do clan</gray>",
                    " ",
                    " <white>" + clan.getMembers().size() + " membros</white>",
                    " "
                )
                .build(), e -> {
                ctx.getPlayer().closeInventory();
                MenuUtil.openMenu(player, ClanMembersMenu.class);
            }
        );

        // ── Settings (owner only) ──
        if (isOwner) {
            ctx.setItem(
                'S', ItemBuilder.of(Material.COMPARATOR)
                    .name("<aqua><bold>⚙ Configurações</bold></aqua>")
                    .addLore(
                        " ",
                        " <gray>Gerencie convites, permissões</gray>",
                        " <gray>e configurações do clan</gray>",
                        " ",
                        " <yellow>Clique para abrir</yellow>",
                        " "
                    )
                    .build(), e -> {
                    ctx.getPlayer().closeInventory();
                    MenuUtil.openMenu(player, ClanSettingsMenu.class);
                }
            );
        }

        // ── Disband / Leave ──
        if (isOwner) {
            ctx.setItem(
                'D', p -> ItemBuilder.of(Material.BARRIER)
                    .name("<red><bold>✖ Desfazer Clan</bold></red>")
                    .addLore(
                        " ",
                        " <dark_red>⚠ ATENÇÃO!</dark_red>",
                        " <gray>Esta ação é irreversível!</gray>",
                        " ",
                        " <yellow>Clique para continuar</yellow>",
                        " "
                    )
                    .build(), e -> {
                    if (!Cooldown.setIfNotInCooldownSec(player.getUniqueId(), 2L, "clan_menu_disband")) {
                        player.sendMessage(msg().get("cooldown"));
                        return;
                    }
                    player.closeInventory();
                    Map<String, Object> data = new HashMap<>();
                    data.put(ClanConfirmationMenu.KEY_ACTION, ClanConfirmationMenu.ACTION_DISBAND);
                    MenuUtil.openMenu(player, ClanConfirmationMenu.class, data);
                }
            );
        } else {
            ctx.setItem(
                'D', p -> ItemBuilder.of(Material.OAK_DOOR)
                    .name("<yellow><bold>← Sair do Clan</bold></yellow>")
                    .addLore(
                        " ",
                        " <gray>Clique para sair do clan</gray>",
                        " "
                    )
                    .build(), e -> {
                    if (!Cooldown.setIfNotInCooldownSec(player.getUniqueId(), 3L, "clan_leave")) {
                        player.sendMessage(msg().get("cooldown"));
                        return;
                    }
                    Thread.startVirtualThread(() -> {
                        var result = service.leave(player.getUniqueId());
                        player.closeInventory();
                        player.sendMessage(result.getMessage());
                    });
                }
            );
        }

        // ── Friendly Fire Toggle ──
        if (member != null && member.hasPermission(ClanPermission.PVP_CONTROL)) {
            ctx.setItem(
                'F', p -> ItemBuilder.of(clan.isFriendlyFire() ? Material.REDSTONE_TORCH : Material.TORCH)
                    .name("<gold><bold>⚔ Fogo Amigo</bold></gold>")
                    .addLore(
                        " ",
                        " <gray>Status atual:</gray> " + (clan.isFriendlyFire() ? "<green>Ativado" : "<red>Desativado"),
                        " ",
                        " <gray>Clique para alternar</gray>",
                        " "
                    )
                    .build(), e -> {
                    if (!Cooldown.setIfNotInCooldownSec(player.getUniqueId(), 3L, "friendly_fire_toggle")) {
                        player.sendMessage(msg().get("cooldown"));
                        return;
                    }
                    Thread.startVirtualThread(() -> {
                        var result = service.toggleFriendlyFire(player.getUniqueId());
                        if (result == io.github.minehollow.clans.service.ClanResult.SUCCESS) {
                            ctx.update();
                        } else {
                            player.sendMessage(result.getMessage());
                        }
                    });
                }
            );
        }

        // ── Upgrade Slots ──
        if (member != null && member.hasPermission(ClanPermission.UPGRADES)) {
            int currentTier = clan.getSlotTier();
            int maxTier = plugin.getSlotTable().length;
            boolean canUpgrade = currentTier < maxTier;

            List<String> lore = new ArrayList<>();
            lore.add(" ");
            lore.add(" <gray>Nível atual:</gray> <white>" + currentTier + "</white>");
            lore.add(" <gray>Vagas:</gray> <white>" + clan.getMaxMembers(plugin.getSlotTable()) + "</white>");

            if (canUpgrade) {
                double cost = plugin.getConfig().getDouble("upgrades.slots." + (currentTier + 1) + ".cost", 0);
                int nextSlots = plugin.getSlotTable()[currentTier];
                lore.add(" ");
                lore.add(" <yellow>Próximo nível:</yellow>");
                lore.add(" <gray>Vagas:</gray> <white>" + nextSlots + "</white>");
                lore.add(" <gray>Custo:</gray> <green>$" + String.format("%.0f", cost) + "</green>");
                lore.add(" ");
                lore.add(" <yellow>Clique para melhorar</yellow>");
            } else {
                lore.add(" ");
                lore.add(" <green>Nível máximo!</green>");
            }
            lore.add(" ");

            ItemBuilder builder = ItemBuilder.of(canUpgrade ? Material.EMERALD : Material.EMERALD_BLOCK)
                .name("<green><bold>⬆ Melhorar Vagas</bold></green>")
                .addLore(lore.toArray(new String[0]));

            if (canUpgrade) builder.glow();

            ctx.setItem(
                'U', p -> builder.build(), e -> {
                    if (!canUpgrade) {
                        player.sendMessage(msg().get("max-tier-reached"));
                        return;
                    }
                    if (!Cooldown.setIfNotInCooldownSec(player.getUniqueId(), 2L, "clan_menu_upgrade")) {
                        player.sendMessage(msg().get("cooldown"));
                        return;
                    }
                    player.closeInventory();
                    player.performCommand("clan melhorar");
                }
            );
        }

        return true;
    }

    private String resolvePlayerName(java.util.UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        var offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString().substring(0, 8);
    }
}

