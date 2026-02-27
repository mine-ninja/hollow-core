package io.github.minehollow.clans.menu;

import io.github.minehollow.clans.ClansPlugin;
import io.github.minehollow.clans.config.MessageConfig;
import io.github.minehollow.clans.model.Clan;
import io.github.minehollow.clans.service.ClanResult;
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

import java.util.UUID;

/**
 * Reusable confirmation menu for destructive clan actions.
 * <p>
 * Pass data keys:
 * <ul>
 *   <li>{@code confirm_action} — one of {@code "disband"} or {@code "transfer"}</li>
 *   <li>{@code transfer_target} — (transfer only) UUID of the new owner</li>
 * </ul>
 */
@RequiredArgsConstructor
public class ClanConfirmationMenu extends SimpleMenu {

    public static final String KEY_ACTION = "confirm_action";
    public static final String KEY_TRANSFER_TARGET = "transfer_target";

    public static final String ACTION_DISBAND = "disband";
    public static final String ACTION_TRANSFER = "transfer";

    private final ClansPlugin plugin;

    private MessageConfig msg() {
        return plugin.getMessageConfig();
    }

    @Override
    public boolean onPreOpen(@NotNull PlayerMenuContext ctx, @NotNull MenuConfig openHandler) {
        Player player = ctx.getPlayer();
        ClanService service = plugin.getClanService();

        String action = ctx.get(KEY_ACTION);
        if (action == null) {
            player.sendMessage(msg().get("confirmation.invalid-action"));
            return false;
        }

        Clan clan = service.getByPlayer(player.getUniqueId());
        if (clan == null) {
            player.sendMessage(msg().get("menu.not-in-clan"));
            return false;
        }

        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(msg().get("confirmation.not-owner"));
            return false;
        }

        // Build title & description based on action
        String title;
        String[] description;

        if (action.equals(ACTION_DISBAND)) {
            title = "<red><bold>Desfazer Clan?</bold></red>";
            description = new String[]{
                " ",
                " <dark_red><bold>⚠ ATENÇÃO!</bold></dark_red>",
                " ",
                " <gray>Você está prestes a desfazer o clan</gray>",
                " <white><bold>[" + clan.getTag() + "]</bold> " + clan.getName() + "</white>",
                " ",
                " <gray>Todos os membros serão removidos.</gray>",
                " <gray>Esta ação é <red>irreversível</red>!</gray>",
                " "
            };
        } else if (action.equals(ACTION_TRANSFER)) {
            UUID targetUuid = ctx.get(KEY_TRANSFER_TARGET);
            if (targetUuid == null) {
                player.sendMessage(msg().get("confirmation.target-not-defined"));
                return false;
            }
            String targetName = resolvePlayerName(targetUuid);
            title = "<gold><bold>Transferir Liderança?</bold></gold>";
            description = new String[]{
                " ",
                " <gold><bold>👑 TRANSFERÊNCIA</bold></gold>",
                " ",
                " <gray>Você está prestes a transferir a</gray>",
                " <gray>liderança do clan para:</gray>",
                " ",
                " <white><bold>" + targetName + "</bold></white>",
                " ",
                " <gray>Você perderá o cargo de dono.</gray>",
                " <gray>Esta ação é <red>irreversível</red>!</gray>",
                " "
            };
        } else {
            player.sendMessage(msg().get("confirmation.unknown-action"));
            return false;
        }

        openHandler.setTitle(StringUtils.text(title));
        openHandler.setRows(3);
        openHandler.setLayout(
            "         ",
            " C   D X ",
            "         "
        );

        // ── Description item ──
        ctx.setItem('D', ItemBuilder.of(action.equals(ACTION_DISBAND) ? Material.TNT : Material.GOLDEN_HELMET)
            .name(action.equals(ACTION_DISBAND)
                ? "<red><bold>✖ Desfazer Clan</bold></red>"
                : "<gold><bold>👑 Transferir Liderança</bold></gold>")
            .addLore(description)
            .flags(ItemFlag.HIDE_ATTRIBUTES)
            .build()
        );

        // ── Confirm button ──
        ctx.setItem('C', ItemBuilder.of(Material.LIME_CONCRETE)
            .name("<green><bold>✔ Confirmar</bold></green>")
            .addLore(
                " ",
                " <gray>Clique para confirmar a ação</gray>",
                " "
            )
            .build(), e -> executeAction(ctx, player, service, clan, action)
        );

        // ── Cancel button ──
        ctx.setItem('X', ItemBuilder.of(Material.RED_CONCRETE)
            .name("<red><bold>✖ Cancelar</bold></red>")
            .addLore(
                " ",
                " <gray>Clique para cancelar</gray>",
                " "
            )
            .build(), e -> {
                player.closeInventory();
                MenuUtil.openMenu(player, ClanMainMenu.class);
            }
        );

        return true;
    }

    private void executeAction(
        @NotNull PlayerMenuContext ctx,
        @NotNull Player player,
        @NotNull ClanService service,
        @NotNull Clan clan,
        @NotNull String action
    ) {
        player.closeInventory();

        Thread.startVirtualThread(() -> {
            if (action.equals(ACTION_DISBAND)) {
                ClanResult result = service.disband(player.getUniqueId());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (result == ClanResult.SUCCESS) {
                        msg().sendList(player, "disband-success", "tag", clan.getTag());
                    } else {
                        player.sendMessage(result.getMessage());
                    }
                });
            } else if (action.equals(ACTION_TRANSFER)) {
                UUID targetUuid = ctx.get(KEY_TRANSFER_TARGET);
                if (targetUuid == null) {
                    player.sendMessage(msg().get("confirmation.internal-error"));
                    return;
                }

                ClanResult result = service.transferOwnership(player.getUniqueId(), targetUuid);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (result == ClanResult.SUCCESS) {
                        String targetName = resolvePlayerName(targetUuid);
                        msg().sendList(player, "transfer-success", "player", targetName);
                        Player target = Bukkit.getPlayer(targetUuid);
                        if (target != null) {
                            msg().sendList(target, "transfer-target", "tag", clan.getTag());
                        }
                    } else {
                        player.sendMessage(result.getMessage());
                    }
                });
            }
        });
    }

    private @NotNull String resolvePlayerName(@NotNull UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        var offline = Bukkit.getOfflinePlayer(uuid);
        return offline.getName() != null ? offline.getName() : uuid.toString().substring(0, 8);
    }
}

