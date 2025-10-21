package net.warcane.lugin.core.minecraft.mailbox;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.BukkitPlatform;
import net.warcane.lugin.core.minecraft.BukkitPlatformPlugin;
import net.warcane.lugin.core.minecraft.gamerule.CustomGameRule;
import net.warcane.lugin.core.minecraft.gamerule.GameRuleRegistry;
import net.warcane.lugin.core.minecraft.mailbox.commands.MailCommand;
import net.warcane.lugin.core.minecraft.mailbox.data.MailData;
import net.warcane.lugin.core.minecraft.mailbox.data.MailItem;
import net.warcane.lugin.core.minecraft.mailbox.inv.MailboxMenu;
import net.warcane.lugin.core.minecraft.mailbox.repository.MailItemRepository;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Slf4j
public class MailManager {

    public static final CustomGameRule<Boolean> DISABLE_MAIL = new CustomGameRule<>(
        "disableMail",
        Boolean.class,
        false,
        "Disables hunger depletion for all players"
    );

    private final MailItemRepository repository;

    private static MailManager instance;

    public MailManager() {
        this.repository = new MailItemRepository();
        GameRuleRegistry.register(DISABLE_MAIL);

        BukkitPlatform.getInstance().getMenuManager().register(
            new MailboxMenu(this)
        );
        BukkitPlatformPlugin.getInstance().registerCommands("mail", new MailCommand(this));
    }

    public void addMailItem(@NotNull UUID player, MailItem item) {
        repository.addMail(player, item);
    }

    public CompletableFuture<Boolean> removeMailItem(@NotNull UUID player, UUID mailId) {
        return repository.removeMail(player, mailId);
    }

    public CompletableFuture<Boolean> removeMailItem(UUID player, MailItem data) {
        return repository.removeMail(player, data.getMailId());
    }

    public CompletableFuture<MailData> getMailData(@NotNull UUID player) {
        return repository.findByUniqueId(player);
    }

    public static void init(Plugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("PunishManager is already initialized.");
        }
        instance = new MailManager();
        log.info("MailManager initialized.");
    }

    public static MailManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MailManager is not initialized yet.");
        }
        return instance;
    }
}
