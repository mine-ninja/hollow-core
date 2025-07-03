package net.warcane.lugin.core.util.data;

import net.warcane.lugin.core.database.RedisConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Representa um cache de dados utilizando Redis para armazenar objetos.
 *
 * @param <O> tipo generico que representa o objeto a ser armazenado no cache.
 */
public class RedisCache<O> {


    protected final RedisConnector connector;
    protected final GenericSerializer<O> serializer;

//    private final Map<String , Function<O , ?>> indexes = new HashMap<>();

    public RedisCache(@NotNull Class<O> clazz) {
        this(RedisConnector.getInstance(), GenericSerializer.jsonSerializer(clazz));
    }

    public RedisCache(@NotNull GenericSerializer<O> serializer) {
        this(RedisConnector.getInstance(), serializer);
    }

    public RedisCache(@NotNull RedisConnector connector, @NotNull GenericSerializer<O> serializer) {
        this.connector = connector;
        this.serializer = serializer;
    }

    public void hset(@NotNull String key, @NotNull String field, @NotNull O value) {
        connector.useJedis(jedis -> jedis.hset(key, field, serializer.serialize(value)));
    }

    @Nullable
    public O hget(@NotNull String key, @NotNull String field, @NotNull Supplier<@Nullable O> defaultValueSupplier) {
        final var value = hget(key, field);
        if (value != null) return value;

        final var defaultValue = defaultValueSupplier.get();
        if (defaultValue != null) {
            hset(key, field, defaultValue);
        }
        return defaultValue;
    }

    @Nullable
    public O hget(@NotNull String key, @NotNull String field) {
        return connector.supplyFromJedis(jedis -> {
            final var rawData = jedis.hget(key, field);
            return rawData == null ? null : serializer.deserialize(rawData);
        });
    }


    public void hdel(@NotNull String key, @NotNull String field) {
        connector.useJedis(jedis -> jedis.hdel(key, field));
    }

    @NotNull
    public RedisConnector getConnector() {
        return connector;
    }
}
