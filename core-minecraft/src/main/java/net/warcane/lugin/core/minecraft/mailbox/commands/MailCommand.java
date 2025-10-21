package net.warcane.lugin.core.minecraft.mailbox.commands;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.command.SimpleCommand;
import net.warcane.lugin.core.minecraft.command.context.CommandContext;
import net.warcane.lugin.core.minecraft.command.exception.CommandFailedException;
import net.warcane.lugin.core.minecraft.mailbox.MailManager;
import net.warcane.lugin.core.minecraft.mailbox.data.MailItem;
import net.warcane.lugin.core.minecraft.util.message.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
        StringUtils.send(audience, "<l-loading>Carregando sua caixa de correio...");
        mailManager.getMailData(ctx.getSenderAsPlayer().getUniqueId()).whenComplete((mailData, throwable) ->  {
            if (throwable != null) {
                StringUtils.send(audience, "<l-error>Ocorreu um erro ao carregar sua caixa de correio. Tente novamente mais tarde.");
                throwable.printStackTrace();
                return;
            }
            if (mailData == null) {
                StringUtils.send(audience, "<l-error>Ocorreu um erro ao carregar sua caixa de correio. Tente novamente mais tarde.");
                return;
            }
            List<MailItem> mails = mailData.getMails();
            if (mails.isEmpty()) {
                StringUtils.send(audience, "<l-info>Você não possui nenhum item na sua caixa de correio.");
                return;
            }
            mails = mails.stream().filter(mailItem -> BukkitPlatform.getInstance().getGameServer().serverId().startsWith(mailItem.getServerId())).toList();
            if (mails.isEmpty()) {
                StringUtils.send(audience, "<l-info>Você não pode resgatar nenhum item neste servidor.");
                return;
            }



        });
    }
}
