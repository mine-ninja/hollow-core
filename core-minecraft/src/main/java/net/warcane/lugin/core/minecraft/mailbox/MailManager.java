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
import net.warcane.lugin.core.minecraft.mailbox.events.PlayerJoinNotificationEvent;
import net.warcane.lugin.core.minecraft.mailbox.inv.MailboxMenu;
import net.warcane.lugin.core.minecraft.mailbox.repository.MailItemRepository;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Slf4j
public class MailManager {

    public static final CustomGameRule<Boolean> DISABLE_MAIL = new CustomGameRule<>(
        "disableMail",
        Boolean.class,
        false,
        "Disables the mail system for all players",
        true
    );

    private final MailItemRepository repository;

    private static MailManager instance;

    public MailManager() {
        this.repository = new MailItemRepository();
        GameRuleRegistry.register(DISABLE_MAIL);

        BukkitPlatform pluginInstance = BukkitPlatform.getInstance();
        pluginInstance.getMenuManager().register(
            new MailboxMenu(this)
        );
        Plugin plugin = pluginInstance.getPlugin();
        plugin.getServer().getPluginManager().registerEvents(new PlayerJoinNotificationEvent(this), plugin);
        BukkitPlatformPlugin.getInstance().registerCommands("mail", new MailCommand(this));
    }

    public void addMailItem(@NotNull UUID player, MailItem item) {
        repository.addMail(player, item);
    }

    public CompletableFuture<Boolean> removeMailItem(@NotNull UUID player, UUID mailId) {
        return repository.removeMail(player, mailId);
    }

    public CompletableFuture<Boolean> bulkRemoveMailItems(@NotNull UUID player, List<UUID> mailIds) {
        return repository.bulkRemoveMailItems(player, mailIds);
    }

    public CompletableFuture<Boolean> removeMailItem(UUID player, MailItem data) {
        return repository.removeMail(player, data.getMailId());
    }

    public CompletableFuture<MailData> getMailData(@NotNull UUID player) {
        return repository.findByUniqueId(player);
    }

    public static void init(Plugin plugin) {
        if (instance != null) {
            throw new IllegalStateException("MailManager is already initialized.");
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
