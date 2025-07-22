package net.warcane.lugin.core.util.data;

import com.mongodb.Function;
import com.mongodb.TransactionOptions;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.TransactionBody;
import com.mongodb.client.model.*;
import net.warcane.lugin.core.database.MongoDbConnector;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MongoRepository<ID, O> {

    private static final FindOneAndReplaceOptions FIND_ONE_AND_REPLACE_OPTIONS = new FindOneAndReplaceOptions()
      .upsert(true)
      .returnDocument(ReturnDocument.AFTER);

    private static final FindOneAndUpdateOptions FIND_ONE_AND_UPDATE_OPTIONS = new FindOneAndUpdateOptions()
      .upsert(true)
      .returnDocument(ReturnDocument.AFTER);

    private final MongoCollection<O> collection;
    private final MongoCollection<Document> rawCollection;
    private final String idFieldName;


    public MongoRepository(Class<O> clazz, String idFieldName) {
        this.collection = MongoDbConnector.getInstance().getCollection(clazz.getSimpleName(), clazz);
        this.rawCollection = MongoDbConnector.getInstance().getCollection(clazz.getSimpleName(), Document.class);
        this.idFieldName = idFieldName;
    }

    public MongoRepository(@NotNull MongoCollection<O> collection, @NotNull String idFieldName) {
        this.collection = collection;
        this.rawCollection = MongoDbConnector.getInstance().getCollection(collection.getNamespace().getCollectionName(), Document.class);
        this.idFieldName = idFieldName;
    }

    public O findById(ID id) {
        return this.findFirstFromProperty(idFieldName, id);
    }

    public O findById(@NotNull ID id, @NotNull Supplier<O> defaultValueSupplier) {
        final var object = this.findById(id);
        if (object == null) {
            return defaultValueSupplier.get();
        }
        return object;
    }

    public O save(O object, Function<O, ID> idExtractor) {
        final var id = idExtractor.apply(object);
        return collection.findOneAndReplace(Filters.eq(idFieldName, id), object, FIND_ONE_AND_REPLACE_OPTIONS);
    }

    public O set(@NotNull Bson filter, @NotNull String key, @NotNull Object value) {
        return collection.findOneAndUpdate(filter, Updates.set(key, value), FIND_ONE_AND_UPDATE_OPTIONS);
    }

    public O findFirstFromProperty(@NotNull String key, @NotNull Object value) {
        return collection.find(Filters.eq(key, value)).first();
    }

    public O findFirstFromPropertyIgnoreCase(@NotNull String key, @NotNull String value) {
        return collection.find(Filters.regex(key, value, "i")).first();
    }

    public List<O> queryAll() {
        return collection.find().into(new ArrayList<>());
    }


    public <T> T supplyFromCollection(@NotNull Function<MongoCollection<O>, T> function) {
        return function.apply(collection);
    }

    public void useCollection(@NotNull Consumer<MongoCollection<O>> consumer) {
        consumer.accept(collection);
    }


    public <T> CompletableFuture<T> executeTransaction(@NotNull TransactionBody<T> transactionBody, @NotNull ExecutorService executorService) {
        return CompletableFuture.supplyAsync(() -> {
            try (var session = MongoDbConnector.getInstance().getMongoClient().startSession()) {
                return session.withTransaction(transactionBody);
            } catch (Exception e) {
                throw new RuntimeException("Erro ao executar transação: " + e.getMessage(), e);
            }
        }, executorService);
    }

    @NotNull
    public MongoCollection<O> getCollection() {
        return collection;
    }


    @NotNull
    public MongoCollection<Document> getRawCollection() {
        return rawCollection;
    }
}
