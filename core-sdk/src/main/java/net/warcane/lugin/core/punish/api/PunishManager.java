package net.warcane.lugin.core.punish.api;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import lombok.extern.log4j.Log4j2;
import net.warcane.lugin.core.database.MongoDbConnector;
import net.warcane.lugin.core.punish.data.PunishedDTO;

import java.util.UUID;
import java.util.concurrent.*;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/06/2025
 * @project punish
 */
public class PunishManager {
    private static PunishManager punishManager;
    private final MongoCollection<PunishedDTO> collection;

    public PunishManager(MongoDbConnector connector) {
        PunishManager.punishManager = this;

        this.collection = connector.getCollection("punished_players", PunishedDTO.class);
    }

    public CompletableFuture<PunishedDTO> getPunishedPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
            collection.find(Filters.eq("uuid", uuid)).first()
        );
    }

    public static PunishManager get() {
        return punishManager;
    }
}
