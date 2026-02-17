package io.github.minehollow.mailbox;

import io.github.minehollow.mailbox.command.MailboxAdminCommand;
import io.github.minehollow.mailbox.command.MailboxCommand;
import io.github.minehollow.mailbox.menu.BoxPreviewMenu;
import io.github.minehollow.mailbox.menu.MailboxMenu;
import io.github.minehollow.mailbox.service.MailboxService;
import io.github.minehollow.minecraft.menu.MenuUtil;
import io.github.minehollow.minecraft.plugin.SimplePlugin;
import lombok.Getter;

@Getter
public class MailboxPlugin extends SimplePlugin {
    private MailboxService mailboxService;

    @Override
    public void onEnable() {
        mailboxService = new MailboxService(this);

        registerCommands("mailbox",
                new MailboxCommand(this),
                new MailboxAdminCommand(this));

        MenuUtil.registerMenus(
                new MailboxMenu(this),
                new BoxPreviewMenu(this));
    }
}
