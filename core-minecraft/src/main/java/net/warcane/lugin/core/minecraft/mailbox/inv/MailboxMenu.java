package net.warcane.lugin.core.minecraft.mailbox.inv;

import net.kyori.adventure.text.Component;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.integration.Compat;
import net.warcane.lugin.core.minecraft.integration.CoreProtect;
import net.warcane.lugin.core.minecraft.mailbox.MailManager;
import net.warcane.lugin.core.minecraft.mailbox.data.MailData;
import net.warcane.lugin.core.minecraft.mailbox.data.MailItem;
import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import net.warcane.lugin.core.minecraft.menu.pagination.MenuPaginationContext;
import net.warcane.lugin.core.minecraft.menu.pagination.SimplePaginationMenu;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import net.warcane.lugin.core.minecraft.util.sound.PredefinedSound;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/10/2025
 * @project LUGIN
 */
public class MailboxMenu extends SimplePaginationMenu<MailItem> {

    private final MailManager repository;

    public MailboxMenu(@NotNull MailManager mailManager) {
        this.repository = mailManager;
    }

    @Override
    public boolean onPreOpen(@NotNull MenuPaginationContext<MailItem> ctx, @NotNull MenuConfig openHandler) {
        openHandler.setClickSound(new PredefinedSound(Sound.UI_BUTTON_CLICK, 0.5F, 1.0F));
        openHandler.setLayout(
            "xxxxxxxxx",
            "xxxxxxxxx",
            "xxxxxxxxx",
            " p rrr n ");

        MailData mailData = ctx.get("mail_data");
        boolean isAdmin = ctx.getOrDefault("is_admin_view", false);
        if (mailData == null) {
            return false;
        }
        // TODO: Rodrigo da uma olhada, por algum motivo ele só insere o pagination se estiver async
        Tasks.runAsync(() -> ctx.setPagination('x', mailData.getMails(), (player, mail) -> mail.getDisplayItem(), (mail, event) -> {
            if (!isAdmin) return;
            repository.removeMailItem(mailData.getUniqueId(), mail.getMailId());
            StringUtils.send(event.getWhoClicked(), "<l-success>Item removido com sucesso da caixa de correio do jogador.");
            ctx.close();
        }));

        boolean runningOnNewVersions = BukkitPlatform.getInstance().isRunningOnNewVersions();

        ctx.setItem('r', runningOnNewVersions ? getNewRedeemItem() : getOldRedeemItem(), (event) -> {

            if (isAdmin) {
                StringUtils.send(event.getWhoClicked(), "<l-negate>Você não pode resgatar itens no modo de visualização de administrador.");
                return;
            }
            PlayerInventory inventory = event.getWhoClicked().getInventory();


            List<MailItem> mailsToRemove = new ArrayList<>();
            boolean checkedInventorySpace = false;
            for (MailItem item : mailData.getMails()) {
                if (!item.canAddToPlayerInv(inventory)) continue;

                // Verifica se precisa checar espaço no inventário
                if (item.needToCheckInventorySpace() && !checkedInventorySpace) {
                    int slotsEmpty = 0;

                    for (ItemStack itemStack : inventory.getContents()) {
                        if (itemStack == null || itemStack.getType() == Material.AIR) {
                            slotsEmpty++;
                        }
                    }
                    if (slotsEmpty == 0) {
                        StringUtils.send(event.getWhoClicked(), "<l-negate>Você não possui espaço suficiente no inventário para resgatar seus itens.");
                        ctx.close();
                        return;
                    }
                    checkedInventorySpace = true;
                }
                // ----

                mailsToRemove.add(item);
            }
            repository.bulkRemoveMailItems(event.getWhoClicked().getUniqueId(), mailsToRemove.stream().map(MailItem::getMailId).toList()).whenComplete((saved, throwable) -> {
                if (throwable != null || !saved) {
                    StringUtils.send(event.getWhoClicked(), "<l-negate>Ocorreu um erro ao resgatar seus itens. Tente novamente mais tarde.");
                    return;
                }
                handleCoreProtect((Player) event.getWhoClicked(), mailsToRemove);
                for (MailItem item : mailsToRemove) {
                    inventory.addItem(item.getContents());
                }
            });
            StringUtils.send(event.getWhoClicked(), "<l-confirm>Você resgatou sua caixa de correio com sucesso!");
            ctx.close();
        });

        if (runningOnNewVersions) {
            return onPreOpenInNewVersion(ctx, openHandler, mailData, isAdmin);
        }
        return onOldVersion(ctx, openHandler, mailData, isAdmin);
    }


    private boolean onPreOpenInNewVersion(@NotNull MenuPaginationContext<MailItem> ctx, @NotNull MenuConfig openHandler, MailData mailData, boolean isAdmin) {
        openHandler.setTitle(StringUtils.text("<font:lugin:negative_padding_nosplit>\uE030<white><font:lugin:chests>\uE003"));
        ItemStack nextItem = new ItemStack(Material.ECHO_SHARD);
        nextItem.editMeta(meta -> {
            meta.setCustomModelData(42);
            meta.displayName(StringUtils.formItemName("<l-yellow>Próxima Página"));
        });
        ctx.setNextButton('n', nextItem);

        ItemStack previousItem = new ItemStack(Material.ECHO_SHARD);
        previousItem.editMeta(meta -> {
            meta.setCustomModelData(41);
            meta.displayName(StringUtils.formItemName("<l-yellow>Página Anterior"));
        });
        ctx.setPreviousButton('p', previousItem);
        return true;
    }

    private boolean onOldVersion(@NotNull MenuPaginationContext<MailItem> ctx, @NotNull MenuConfig openHandler, MailData mailData, boolean isAdminOnly) {
        // TODO: essa eu deixo para meu amigo alvaro luis inácio da silva
        return false;
    }


    private ItemStack getNewRedeemItem() {
        ItemStack redeemItem = new ItemStack(Material.ECHO_SHARD);
        redeemItem.editMeta(meta -> {
            meta.setCustomModelData(44);
            meta.displayName(StringUtils.formItemName("<l-green>RESGATAR"));
        });
        return redeemItem;
    }

    private ItemStack getOldRedeemItem() {
        ItemStack redeemItem = new ItemStack(Material.PAPER);
        // TODO: essa eu deixo para meu amigo alvaro luis inácio da silva
        return redeemItem;
    }


    private void handleCoreProtect(Player player, List<MailItem> mailItems) {
        Compat.CORE_PROTECT.runIfPresent(
            () -> () ->  {
                for (MailItem mailItem : mailItems) {
                    for (ItemStack itemStack : mailItem.getContents()) {
                        CoreProtect.log(player, "Resgatou item da caixa de correio: " + itemStack.getType().name() + " x" + itemStack.getAmount());
                    }
                }
            }
        );
    }
}
