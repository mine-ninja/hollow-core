package net.warcane.lugin.core.util.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.warcane.lugin.core.database.RedisConnector;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Representa um cache de dados utilizando Redis para armazenar objetos.
 *
 * @param <O> tipo generico que representa o objeto a ser armazenado no cache.
 */
public class RedisCache<O> {
    protected final RedisConnector connector;
    @Getter protected final GenericSerializer<O> serializer;
    
    private volatile String setAndGetSha;
    private volatile String dualCacheSha;
    private volatile String multiSetSha;
    
    private static final String HSET_AND_GET_SCRIPT = """
        redis.call('HSET', KEYS[1], KEYS[2], ARGV[1])
        return redis.call('HGET', KEYS[1], KEYS[2])
    """;
    private static final String CACHE_DUAL_KEY_SCRIPT = """
        redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])
        redis.call('HSET', KEYS[1], ARGV[2], ARGV[3])
        return 1
    """;
    private static final String MULTI_SET_SCRIPT = """
        local key = KEYS[1]
        local value = ARGV[1]
        local numFields = tonumber(ARGV[2])
        for i = 3, 2 + numFields do
            redis.call('HSET', key, ARGV[i], value)
        end
        return numFields
    """;
    
    public RedisCache(@NotNull Class<O> clazz) {
        this(RedisConnector.getInstance(), GenericSerializer.jsonSerializer(clazz));
    }
    
    public RedisCache(@NotNull GenericSerializer<O> serializer) {
        this(RedisConnector.getInstance(), serializer);
    }
    
    public RedisCache(@NotNull RedisConnector connector, @NotNull GenericSerializer<O> serializer) {
        this.connector = connector;
        this.serializer = serializer;
        
        this.connector.useJedis(jedis -> {
            this.setAndGetSha = jedis.scriptLoad(HSET_AND_GET_SCRIPT);
            this.dualCacheSha = jedis.scriptLoad(CACHE_DUAL_KEY_SCRIPT);
            this.multiSetSha = jedis.scriptLoad(MULTI_SET_SCRIPT);
        });
    }
    
    @Nullable
    public O hSetAndGet(@NotNull String key, String field, O value) {
        return connector.supplyFromJedis(jedis -> {
            final var result = (String) jedis.evalsha(setAndGetSha, 2, key, field, serializer.serialize(value));
            return result == null ? null : serializer.deserialize(result);
        });
    }
    
    /**
     * Atomically sets a value in a hash with two different fields (e.g., UUID and name).
     * Both fields will point to the same serialized value.
     *
     * @param key    Hash key
     * @param field1 First field (e.g., UUID)
     * @param field2 Second field (e.g., lowercase name)
     * @param value  Value to store
     */
    public void hsetDualKey(@NotNull String key, @NotNull String field1, @NotNull String field2, @NotNull O value) {
        connector.useJedis(jedis -> jedis.evalsha(dualCacheSha, 1, key, field1, field2, serializer.serialize(value)));
    }
    
    public void hsetMultiField(@NotNull String key, @NotNull List<String> fields, @NotNull O value) {
        if (fields.isEmpty()) throw new IllegalArgumentException("Fields list cannot be empty");
        
        connector.useJedis(jedis -> {
            final List<String> args = new ArrayList<>(2 + fields.size());
            args.add(serializer.serialize(value));
            args.add(String.valueOf(fields.size()));
            args.addAll(fields);
            jedis.evalsha(multiSetSha, List.of(key), args);
        });
    }
    
    public void set(@NotNull String key, @NotNull O value, long seconds) {
        connector.useJedis(jedis -> {
            jedis.set(key, serializer.serialize(value));
            jedis.expire(key, seconds);
        });
    }
    
    public void set(@NotNull String key, @NotNull O value) {
        connector.useJedis(jedis -> jedis.set(key, serializer.serialize(value)));
    }
    
    public void del(@NotNull String key) {
        connector.useJedis(jedis -> jedis.del(key));
    }
    
    @Nullable
    public O get(@NotNull String key) {
        return connector.supplyFromJedis(jedis -> {
            final var rawData = jedis.get(key);
            return rawData == null ? null : serializer.deserialize(rawData);
        });
    }
    
    @Nullable
    public O get(@NotNull String key, @NotNull Supplier<@Nullable O> defaultValueSupplier) {
        final var value = get(key);
        if (value != null) return value;
        
        final var defaultValue = defaultValueSupplier.get();
        if (defaultValue != null) {
            set(key, defaultValue);
        }
        return defaultValue;
    }
    
    public void hset(String key, Map<String, O> fieldValues) {
        Map<String, String> result = new Object2ObjectOpenHashMap<>();
        fieldValues.forEach((field, value) -> result.put(field, serializer.serialize(value)));
        
        connector.useJedis(jedis -> jedis.hset(key, result));
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
    
    @NotNull
    public O hget(@NotNull String key, @NotNull String field, @NotNull O defaultValue) {
        final var value = hget(key, field);
        if (value != null) return value;
        
        hset(key, field, defaultValue);
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
    
    public List<O> hgetAll(@NotNull String key) {
        return connector.supplyFromJedis(jedis -> {
            var rawData = jedis.hgetAll(key);
            return rawData.values().stream().map(serializer::deserialize).toList();
        });
    }
    
    @NotNull
    public RedisConnector getConnector() {
        return connector;
    }
}
