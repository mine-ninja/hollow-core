package io.github.minehollow.sdk.player.account.data;

import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.minehollow.sdk.util.JsonUtil.JSON_MAPPER;

@Slf4j
public record ScopedData(
    @BsonProperty("data") Map<String, Object> dataMap
) {

    public ScopedData() {
        this(new ConcurrentHashMap<>());
    }

    public <T> void set(@NotNull DataKey<T> key, T value) {
        dataMap.put(key.id(), value);
    }

    public <T> void setById(String id, T value) {
        dataMap.put(id, value);
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<T> getValue(String id, TypeToken<T> type) {
        Object value = dataMap.get(id);
        if (value == null) return Optional.empty();

        if (value instanceof String strValue) {
            if (type.getRawType().equals(String.class)) return Optional.of((T) strValue);

            try {
                T deserializedValue = JSON_MAPPER.readValue(strValue, JSON_MAPPER.constructType(type.getType()));
                return Optional.of(deserializedValue);
            } catch (Exception e) {
                log.error("Failed to deserialize ScopedData value for id '{}': {}", id, e.getMessage(), e);
                return Optional.empty();
            }
        }

        return Optional.of((T) value);
    }

    public <T> Optional<T> get(@NotNull DataKey<T> key) {
        return getValue(key.id(), key.type());
    }

    public <T> Optional<T> getById(String id, TypeToken<T> type) {
        return getValue(id, type);
    }

    public <T> T getOrDefault(String id, TypeToken<T> typeToken, T defaultValue) {
        return getById(id, typeToken).orElse(defaultValue);
    }

    public <T> T getOrDefault(DataKey<T> key, T defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public <T> boolean has(String id) {
        return dataMap.containsKey(id);
    }

    public <T> boolean has(@NotNull DataKey<T> key) {
        return dataMap.containsKey(key.id());
    }

    public <T> void remove(@NotNull DataKey<T> key) {
        dataMap.remove(key.id());
    }
}
