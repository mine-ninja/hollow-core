package net.warcane.lugin.core.punish.api;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.punish.data.PunishedDTO;

import java.util.UUID;
import java.util.concurrent.*;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/06/2025
 */
public class PunishManager {
    private static PunishManager punishManager;
    private final MongoCollection<PunishedDTO> collection;
    private final ExecutorService executorService;

    public PunishManager(MongoDbConnector connector, ExecutorService executorService) {
        PunishManager.punishManager = this;
        this.executorService = executorService;

        this.collection = connector.getCollection("punished_players", PunishedDTO.class);
    }

    public CompletableFuture<PunishedDTO> getPunishedPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
            collection.find(Filters.eq("uuid", uuid)).first()
        , executorService);
    }

    private CompletableFuture<PunishedDTO> getPunishedByPunishmentId(int id) {
        return CompletableFuture.supplyAsync(() ->
                collection.find(Filters.elemMatch("punishments", Filters.eq("_id", id))).first()
            , executorService);
    }

    public void updatePunishmentStatus(int id, PunishedDTO.Punishment updatedPunishment) {
        getPunishedByPunishmentId(id).whenComplete((punished, e) -> {
            if (e != null || punished == null) {
                return;
            }

            punished.getPunishments().stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .ifPresent(p -> punished.getPunishments().set(punished.getPunishments().indexOf(p), updatedPunishment));

            collection.replaceOne(Filters.eq("uuid", punished.getUuid()), punished, new ReplaceOptions().upsert(true));
        });
    }


    public static PunishManager get() {
        return punishManager;
    }
}
