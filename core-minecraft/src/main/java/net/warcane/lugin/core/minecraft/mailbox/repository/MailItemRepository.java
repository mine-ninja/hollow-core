package net.warcane.lugin.core.minecraft.mailbox.repository;

import com.mongodb.client.model.Indexes;
import net.warcane.lugin.core.minecraft.mailbox.data.MailData;
import net.warcane.lugin.core.minecraft.mailbox.data.MailItem;
import net.warcane.lugin.core.util.data.MongoRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class MailItemRepository {

    private final ExecutorService executorService;
    private final MongoRepository<UUID, MailData> repository = new MongoRepository<>(MailData.class, "uniqueId");

    public MailItemRepository() {
        this.executorService = Executors.newFixedThreadPool(1);

        this.repository.useCollection(collection -> {
            collection.createIndex(Indexes.hashed("uniqueId"));
        });
    }

    public CompletableFuture<MailData> findByUniqueId(@NotNull UUID uniqueId) {
        return supplyAsync(() -> repository.findById(uniqueId), executorService);
    }

    public CompletableFuture<Boolean> addMail(@NotNull UUID playerId, @NotNull MailItem toAdd) {
        executorService.submit(() -> {
            MailData mailData = repository.findById(playerId);
            if (mailData == null) {
                mailData = MailData.create(playerId);
            }
            mailData.getMails().add(toAdd);
            repository.save(mailData, MailData::getUniqueId);
        });
        return null;
    }

    public CompletableFuture<Boolean> removeMail(@NotNull UUID playerId, @NotNull UUID mailIdToRemove) {
        return supplyAsync(() -> {
            MailData mailData = repository.findById(playerId);
            if (mailData == null) {
                return false;
            }
            boolean removed = mailData.getMails().removeIf(mailItem -> mailItem.getMailId().equals(mailIdToRemove));
            if (!removed) {
                return false;
            }
            repository.save(mailData, MailData::getUniqueId);
            return true;
        });
    }

    public CompletableFuture<Boolean> bulkRemoveMailItems(@NotNull UUID playerId, @NotNull List<UUID> mailIdsToRemove) {
        return supplyAsync(() -> {
            MailData mailData = repository.findById(playerId);
            if (mailData == null) {
                return false;
            }
            boolean removed = mailData.getMails().removeIf(mailItem -> mailIdsToRemove.contains(mailItem.getMailId()));
            if (!removed) {
                return false;
            }
            repository.save(mailData, MailData::getUniqueId);
            return true;
        });
    }
}
