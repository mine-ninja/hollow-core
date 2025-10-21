package net.warcane.lugin.core.minecraft.mailbox.commands;

import net.kyori.adventure.audience.Audience;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.mailbox.MailManager;
import net.warcane.lugin.core.minecraft.mailbox.data.MailItem;
import net.warcane.lugin.core.minecraft.mailbox.inv.MailboxMenu;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 21/10/2025
 * @project LUGIN
 */
public class MailCommand extends SimpleCommand {

    private final MailManager mailManager;

    public MailCommand(MailManager mailManager) {
        super("mail");
        setAliases(List.of("correio"));
        this.mailManager = mailManager;
        this.playersOnly = true;
    }

    @Override
    public void performCommand(@NotNull CommandContext ctx) throws CommandFailedException {
        Audience audience = BukkitPlatformPlugin.getInstance().adventure().player(ctx.getSenderAsPlayer());
        BukkitPlatform instance = BukkitPlatform.getInstance();

        if (instance.getGameRuleManager().getGlobalGameRule(MailManager.DISABLE_MAIL)) {
            StringUtils.send(audience, "<l-error>O sistema de correio está desativado neste servidor.");
            return;
        }
        if (ctx.getArgs().length > 0 && ctx.getSender().hasPermission("lugin.mail.admin")) {
            handleAdminCommand(ctx, audience, ctx.getSenderAsPlayer());
            return;
        }
        StringUtils.send(audience, "<l-loading>Carregando sua caixa de correio...");
        mailManager.getMailData(ctx.getSenderAsPlayer().getUniqueId()).whenComplete((mailData, throwable) -> {
            if (throwable != null) {
                StringUtils.send(audience, "<l-error>Ocorreu um erro ao carregar sua caixa de correio. Tente novamente mais tarde.");
                throwable.printStackTrace();
                return;
            }
            if (mailData == null) {
                StringUtils.send(audience, "<l-error>Você não possui nada na sua caixa de correio.");
                return;
            }
            List<MailItem> mails = mailData.getMails();
            if (mails.isEmpty()) {
                StringUtils.send(audience, "<l-info>Você não possui nenhum item na sua caixa de correio.");
                return;
            }
            mails = mails.stream().filter(mailItem -> instance.getGameServer().serverId().startsWith(mailItem.getServerId())).toList();
            if (mails.isEmpty()) {
                StringUtils.send(audience, "<l-info>Você não pode resgatar nenhum item neste servidor.");
                return;
            }

            instance.getMenuManager().openToPlayer(ctx.getSenderAsPlayer(), MailboxMenu.class, Map.of("mail_data", mailData, "is_admin_view", false));

        });
    }

    private void handleAdminCommand(@NotNull CommandContext ctx, Audience audience, Player player) {
        String subCommand = ctx.getRawArgOrNull(0);
        if (ctx.isArgsLength(1) || subCommand == null) {
            StringUtils.send(audience, "<l-info>Uso correto: /mail <add/ver> <jogador>");
            return;
        }
        String playerName = ctx.getRawArgOrNull(1);
        StringUtils.send(audience, "<l-loading>Carregando dados do jogador " + playerName + "...");
        if (playerName == null) {
            StringUtils.send(audience, "<l-error>Uso correto: /mail <add/ver> <jogador>");
            return;
        }
        BukkitPlatform instance = BukkitPlatform.getInstance();
        instance.getPlayerAccountService().getPlayerAccountByName(playerName).whenComplete(((playerAccount, throwable) -> {
            if (throwable != null || playerAccount == null) {
                StringUtils.send(audience, "<l-error>Jogador não encontrado.");
                return;
            }

            UUID targetUUID = playerAccount.uniqueId();
            switch (subCommand.toLowerCase()) {
                case "add" -> {
                    if (player.getInventory().getItemInMainHand().isEmpty()) {
                        StringUtils.send(audience, "<l-error>Você precisa segurar um item na mão principal para enviar.");
                        return;
                    }

                    ItemStack[] itemsToSend = new ItemStack[1];
                    itemsToSend[0] = player.getInventory().getItemInMainHand().clone();
                    MailItem mailItem = MailItem.create(
                        instance.getGameServer().serverId(),
                        itemsToSend
                    );
                    mailManager.addMailItem(targetUUID, mailItem);
                    StringUtils.send(audience, "<l-confirm>Item enviado para a caixa de correio de " + playerAccount.playerName() + " com sucesso.");
                    break;
                }
                case "ver" -> {
                    mailManager.getMailData(targetUUID).whenComplete((mailData, ex) -> {
                        if (ex != null || mailData == null) {
                            StringUtils.send(audience, "<l-error>Ocorreu um erro ao carregar a caixa de correio do jogador.");
                            return;
                        }
                        List<MailItem> mails = mailData.getMails();
                        if (mails.isEmpty()) {
                            StringUtils.send(audience, "<l-info>O jogador " + playerAccount.playerName() + " não possui nenhum item na caixa de correio.");
                            return;
                        }
                        instance.getMenuManager().openToPlayer(player, MailboxMenu.class, Map.of("mail_data", mailData, "is_admin_view", true));
                    });
                }
                default -> StringUtils.send(audience, "<l-error>Subcomando desconhecido. Use 'add' ou 'ver'.");
            }
        }));
    }

    @Override
    public List<String> performTabComplete(@NotNull CommandContext ctx) {
        if (!ctx.getSender().hasPermission("lugin.mail.admin")) return List.of();
        if (ctx.getArgs().length == 1) {
            return List.of("add", "ver");
        }
        return List.of();
    }
}
