package net.warcane.lugin.core.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.warcane.lugin.core.util.codec.mongo.CustomObjectCodecProvider;
import net.warcane.lugin.core.util.data.MongoRepository;
import net.warcane.lugin.core.util.property.Property;
import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MongoDbConnector {


    private static String TABLE_PREFIX = "";
    private static MongoDbConnector instance;

    public static MongoDbConnector getInstance() {
        if (instance == null) {
            instance = fromLocalProperty();
        }
        return instance;
    }

    public static MongoDbConnector fromLocalProperty() {
        final var mongoUrl = Property.getOrThrow("MONGO_URL");
        final var databaseName = Property.get("MONGO_DATABASE", "warcane");
        TABLE_PREFIX = Property.get("MONGO_TABLE_PREFIX", "");
        return new MongoDbConnector(mongoUrl, databaseName);
    }


    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public MongoDbConnector(String connectionString, String databaseName) {
        this(connectionString, databaseName, List.of(), List.of(), List.of());
    }

    private MongoDbConnector(
        @NotNull String connectionString,
        @NotNull String databaseName,
        @NotNull List<CodecProvider> additionalProviders,
        @NotNull List<Codec<?>> additionalCodecs,
        @NotNull List<CodecRegistry> additionalRegistries
    ) {

        CodecRegistry codecRegistry = buildCodecRegistry(additionalProviders, additionalCodecs, additionalRegistries);

        mongoClient = MongoClients.create(
            MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .codecRegistry(codecRegistry)
                .build()
        );

        database = mongoClient.getDatabase(databaseName);
    }

    private CodecRegistry buildCodecRegistry(
        @NotNull List<CodecProvider> additionalProviders,
        @NotNull List<Codec<?>> additionalCodecs,
        @NotNull List<CodecRegistry> additionalRegistries
    ) {

        List<CodecRegistry> registries = new ArrayList<>();
        registries.add(CodecRegistries.fromProviders(new CustomObjectCodecProvider()));
        registries.add(MongoClientSettings.getDefaultCodecRegistry());
        if (!additionalCodecs.isEmpty()) {
            registries.add(CodecRegistries.fromCodecs(additionalCodecs));
        }
        if (!additionalProviders.isEmpty()) {
            registries.add(CodecRegistries.fromProviders(additionalProviders));
        }

        registries.addAll(additionalRegistries);
        registries.add(CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        return CodecRegistries.fromRegistries(registries);
    }

    public <T> MongoCollection<T> getCollection(String collectionName, Class<T> documentClass) {
        return database.getCollection(collectionName, documentClass);
    }

    public <T> MongoCollection<T> getPrefixedCollection(String collectionName, Class<T> documentClass) {
        return database.getCollection(TABLE_PREFIX + collectionName, documentClass);
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @NotNull
    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public <ID, T> MongoRepository<ID, T> createNewRepository(@NotNull Class<T> clazz, @NotNull String idFieldName) {
        return new MongoRepository<>(this, clazz, idFieldName);
    }

    public <ID, T> MongoRepository<ID, T> createNewRepository(
        @NotNull Class<T> clazz,
        @NotNull String idFieldName,
        @NotNull String collectionName
    ) {
        return new MongoRepository<>(this, clazz, idFieldName, collectionName);
    }

    public static Builder builderFromLocalProperty() {
        final var mongoUrl = Property.getOrThrow("MONGO_URL");
        final var databaseName = Property.get("MONGO_DATABASE", "warcane");
        return new Builder(mongoUrl, databaseName);
    }

    public static Builder builder(String connectionString, String databaseName) {
        return new Builder(connectionString, databaseName);
    }

    public static class Builder {
        private final String connectionString;
        private final String databaseName;
        private final List<CodecProvider> additionalProviders = new ArrayList<>();
        private final List<Codec<?>> additionalCodecs = new ArrayList<>();
        private final List<CodecRegistry> additionalRegistries = new ArrayList<>();

        public Builder(String connectionString, String databaseName) {
            this.connectionString = connectionString;
            this.databaseName = databaseName;
        }

        public Builder addCodecProvider(CodecProvider provider) {
            this.additionalProviders.add(provider);
            return this;
        }

        public Builder addCodec(Codec<?> codec) {
            this.additionalCodecs.add(codec);
            return this;
        }

        public Builder addCodecRegistry(CodecRegistry registry) {
            this.additionalRegistries.add(registry);
            return this;
        }

        public MongoDbConnector build() {
            return new MongoDbConnector(connectionString, databaseName,
                additionalProviders, additionalCodecs, additionalRegistries);
        }
    }
}
