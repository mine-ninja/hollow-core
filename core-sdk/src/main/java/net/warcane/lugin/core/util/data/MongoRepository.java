package net.warcane.lugin.core.util.data;

import com.mongodb.Function;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import net.warcane.lugin.core.database.MongoDbConnector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MongoRepository<ID, O> {

    private static final FindOneAndReplaceOptions FIND_ONE_AND_REPLACE_OPTIONS = new FindOneAndReplaceOptions()
      .upsert(true)
      .returnDocument(ReturnDocument.AFTER);

    private final MongoCollection<O> collection;
    private final String idFieldName;

    public MongoRepository(Class<O> clazz, String idFieldName) {
        this.collection = MongoDbConnector.getInstance().getCollection(clazz.getSimpleName(), clazz);
        this.idFieldName = idFieldName;
    }

    public MongoRepository(@NotNull MongoCollection<O> collection, @NotNull String idFieldName) {
        this.collection = collection;
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

    public O findFirstFromProperty(@NotNull String key, @NotNull Object value) {
        return collection.find(Filters.eq(key, value)).first();
    }

    public List<O> queryAll() {
        return collection.find().into(new ArrayList<>());
    }
}
