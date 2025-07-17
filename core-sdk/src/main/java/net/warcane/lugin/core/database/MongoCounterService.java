package net.warcane.lugin.core.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;

/**
 * @author Rok, Pedro Lucas nmm. Created on 02/07/2025
 * @project lugin-core
 */
public class MongoCounterService {

    private static MongoCounterService instance;

    private final MongoCollection<Document> counterCollection;

    private MongoCounterService(MongoDbConnector connector) {
        this.counterCollection = connector.getCollection("counters", Document.class);
    }

    /**
     * Retorna o próximo ID para o contador especificado. <br>
     * * Se o contador não existir, ele será criado e inicializado com 1. <br>
     * Se o contador já existir, ele será incrementado em 1 e o novo valor será retornado.
     * @param counterName o nome do contador para o qual o próximo ID deve ser obtido.
     * @return o próximo ID para o contador especificado.
     */
    public int getNextIdFor(String counterName) {
        Document updated = counterCollection.findOneAndUpdate(
            Filters.eq("_id", counterName),
            Updates.inc("value", 1),
            new FindOneAndUpdateOptions()
                .upsert(true)
                .returnDocument(ReturnDocument.AFTER)
        );
        return updated.getInteger("value");
    }

    public static MongoCounterService get() {
        if (instance == null) {
            instance = new MongoCounterService(MongoDbConnector.getInstance());
        }
        return instance;
    }
}
