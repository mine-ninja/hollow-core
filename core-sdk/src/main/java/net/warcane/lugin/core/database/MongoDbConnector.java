package net.warcane.lugin.core.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import net.warcane.lugin.core.util.property.Property;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jetbrains.annotations.NotNull;

public class MongoDbConnector {

    private static MongoDbConnector instance;

    /**
     * Retorna uma instância singleton do MongoDbConnector.
     *
     * @return Instância do MongoDbConnector.
     */
    public static MongoDbConnector getInstance() {
        if (instance == null) {
            instance = fromLocalProperty();
        }
        return instance;
    }

    public static MongoDbConnector fromLocalProperty() {
        final var mongoUrl = Property.getOrThrow("MONGO_URL");
        final var databaseName = Property.get("MONGO_DATABASE", "warcane");
        return new MongoDbConnector(mongoUrl, databaseName);
    }

    private static final CodecRegistry CODEC_REGISTRY = CodecRegistries.fromRegistries(
      MongoClientSettings.getDefaultCodecRegistry(),
      CodecRegistries.fromProviders(PojoCodecProvider.builder()
        .automatic(true).build())
    );

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public MongoDbConnector(String connectionString, String databaseName) {
        mongoClient = MongoClients.create(
          MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(connectionString))
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .codecRegistry(CODEC_REGISTRY)
            .build()
        );

        database = mongoClient.getDatabase(databaseName);
    }

    public <T> MongoCollection<T> getCollection(String collectionName, Class<T> documentClass) {
        return database.getCollection(collectionName, documentClass);
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
}