package net.warcane.lugin.core.minecraft.mailbox;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.minecraft.mailbox.data.MailData;
import net.warcane.lugin.core.minecraft.mailbox.data.MailItem;
import net.warcane.lugin.core.minecraft.mailbox.repository.MailItemRepository;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Slf4j
public class MailManager {

    private final MailItemRepository repository;

    private static MailManager instance;

    public MailManager() {
        this.repository = new MailItemRepository();
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
