package net.warcane.lugin.core.minecraft.mailbox.inv;

import net.kyori.adventure.text.Component;
import net.minecraft.world.item.Item;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.mailbox.MailManager;
import net.warcane.lugin.core.minecraft.mailbox.data.MailData;
import net.warcane.lugin.core.minecraft.mailbox.data.MailItem;
import net.warcane.lugin.core.minecraft.menu.config.MenuConfig;
import net.warcane.lugin.core.minecraft.menu.pagination.MenuPaginationContext;
import net.warcane.lugin.core.minecraft.menu.pagination.SimplePaginationMenu;
import net.warcane.lugin.core.minecraft.task.Tasks;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/10/2025
 * @project LUGIN
 */
public class MailboxMenu extends SimplePaginationMenu<MailItem> {

    private MailManager repository;

    public MailboxMenu(@NotNull MailManager mailManager) {
        this.repository = mailManager;
    }

    @Override
    public boolean onPreOpen(@NotNull MenuPaginationContext<MailItem> ctx, @NotNull MenuConfig openHandler) {
        openHandler.setLayout(
            "xxxxxxxxx",
            "xxxxxxxxx",
            "xxxxxxxxx",
            " p rrr n ");

        MailData mailData = ctx.get("mail_data");
        if (mailData == null) {
            return false;
        }
        Tasks.runAsync(() -> ctx.setPagination('A', mailData.getMails(), (player, mail) -> mail.getDisplayItem(), (player, mail) -> {

        }));

        if (BukkitPlatform.getInstance().isRunningOnNewVersions()) {
            return onPreOpenInNewVersion(ctx, openHandler, mailData);
        }
        return onOldVersion(ctx, openHandler, mailData);
    }


    private boolean onPreOpenInNewVersion(@NotNull MenuPaginationContext<MailItem> ctx, @NotNull MenuConfig openHandler, MailData mailData) {
        openHandler.setTitle(Component.text());
        ItemStack nextItem = new ItemStack(Material.ECHO_SHARD);
        nextItem.editMeta(meta -> {
           meta.setCustomModelData(42);
           meta.displayName(StringUtils.text("<l-yellow>Próxima Página"));
        });
        ctx.setNextButton('n', nextItem);

        ItemStack previousItem = new ItemStack(Material.ECHO_SHARD);
        previousItem.editMeta(meta -> {
            meta.setCustomModelData(41);
            meta.displayName(StringUtils.text("<l-yellow>Página Anterior"));
        });
        ctx.setPreviousButton('p', previousItem);

        ItemStack redeemItem = new ItemStack(Material.ECHO_SHARD);
        redeemItem.editMeta(meta -> {
            meta.setCustomModelData(1);
            meta.displayName(StringUtils.text("<l-green>RESGATAR"));
        });
        ctx.setItem('r', redeemItem, (event) -> {
            int slotsEmpty = 0;
            PlayerInventory inventory = event.getWhoClicked().getInventory();
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

            for (MailItem item : mailData.getMails()) {
                if (!item.canAddToPlayerInv(inventory)) continue;
                repository.removeMailItem(event.getWhoClicked().getUniqueId(), item.getMailId()).whenComplete((saved, throwable) -> {
                    if (throwable != null || !saved) {
                        StringUtils.send(event.getWhoClicked(), "<l-negate>Ocorreu um erro ao resgatar seus itens. Tente novamente mais tarde.");
                        return;
                    }
                    inventory.addItem(item.getContents());
                });
            }
            StringUtils.send(event.getWhoClicked(), "<l-success>Você resgatou sua caixa de correio com sucesso!");
            ctx.close();
        });
        return true;
    }

    private boolean onOldVersion(@NotNull MenuPaginationContext<MailItem> ctx, @NotNull MenuConfig openHandler, MailData mailData) {
        // TODO: essa eu deixo para meu amigo alvaro luis inácio da silva
        return false;
    }
}
