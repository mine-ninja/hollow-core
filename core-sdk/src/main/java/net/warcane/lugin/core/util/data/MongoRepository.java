package net.warcane.lugin.core.util.data;

import com.mongodb.Function;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.*;
import lombok.extern.slf4j.Slf4j;
import net.warcane.lugin.core.database.MongoDbConnector;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
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


    public MongoRepository(@NotNull Class<O> clazz, @NotNull String idFieldName) {
        this(MongoDbConnector.getInstance(), MongoDbConnector.getInstance().getCollection(clazz.getSimpleName(), clazz), idFieldName);
    }

    public MongoRepository(@NotNull Class<O> clazz, @NotNull String idFieldName, @NotNull String collectionName) {
        this(MongoDbConnector.getInstance(), MongoDbConnector.getInstance().getCollection(collectionName, clazz), idFieldName);
    }

    public MongoRepository(@NotNull MongoDbConnector connector, @NotNull Class<O> clazz, @NotNull String idFieldName) {
        this(connector, connector.getCollection(clazz.getSimpleName(), clazz), idFieldName);
    }

    public MongoRepository(@NotNull MongoDbConnector connector, @NotNull Class<O> clazz, @NotNull String idFieldName, @NotNull String collectionName) {
        this(connector, connector.getCollection(collectionName, clazz), idFieldName);
    }

    public MongoRepository(@NotNull MongoDbConnector connector, @NotNull MongoCollection<O> collection, @NotNull String idFieldName) {
        this.collection = collection;
        this.rawCollection = connector.getCollection(collection.getNamespace().getCollectionName(), Document.class);
        this.idFieldName = idFieldName;
    }

    public MongoRepository(@NotNull MongoDbConnector.Builder builder, @NotNull Class<O> clazz, @NotNull String idFieldName) {
        this(
          builder.build(),
          clazz,
          idFieldName
        );
    }

    public MongoRepository(@NotNull MongoDbConnector.Builder builder, @NotNull Class<O> clazz, @NotNull String idFieldName, @NotNull String collectionName) {
        this(builder.build(), clazz, idFieldName, collectionName);
    }

    public void removeDuplicates(@NotNull String key) {
        final var documents = rawCollection.aggregate(List.of(
          Aggregates.group("$" + key, Accumulators.push("ids", "$_id"), Accumulators.sum("count", 1)),
          Aggregates.match(Filters.gt("count", 1))
        )).into(new ArrayList<>());

        for (Document document : documents) {
            final var ids = (List<Object>) document.get("ids");
            if (ids.size() <= 1) continue;

            final var idsToRemove = ids.subList(1, ids.size());
            rawCollection.deleteMany(Filters.in("_id", idsToRemove));
        }

        log.info("Removed {} duplicate documents based on key '{}'", documents.size(), key);
        rawCollection.createIndex(Indexes.ascending(key), new IndexOptions().unique(true));
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
        return collection.find(Filters.eq(key, value)).first();
    }

    public O deleteById(@NotNull ID id) {
        return collection.findOneAndDelete(Filters.eq(idFieldName, id));
    }

    public O delete(@NotNull O toDelete, @NotNull Function<O, ID> idExtractor) {
        return collection.findOneAndDelete(Filters.eq(idFieldName, idExtractor.apply(toDelete)));
    }

    public List<O> queryMany(@NotNull List<ID> ids) {
        return collection.find(Filters.in(idFieldName, ids)).into(new ArrayList<>());
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

    @NotNull
    public MongoCollection<O> getCollection() {
        return collection;
    }

    @NotNull
    public MongoCollection<Document> getRawCollection() {
        return rawCollection;
    }
}
