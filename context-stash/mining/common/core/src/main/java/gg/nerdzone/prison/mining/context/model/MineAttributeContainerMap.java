/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.context.model;

import gg.nerdzone.prison.mining.api.context.MineAttributeContainer;
import gg.nerdzone.prison.mining.context.util.MineAttributeReflectionUtil;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import oshi.annotation.concurrent.ThreadSafe;

/**
 * An implementation thread-safety and type-safety of the {@link MineAttributeContainer} interface.
 */
public class MineAttributeContainerMap implements MineAttributeContainer {

    private final Map<AttributeKey<?, ?>, AttributeValue<?>> attributes = new ConcurrentHashMap<>();

    @Contract("-> new")
    public static MineAttributeContainerMap create() {
        return new MineAttributeContainerMap();
    }

    @Override
    public <K, V> @Nullable AttributeValue<V> getAttribute(@NotNull AttributeKey<K, V> key) {
        final AttributeValue<?> attributeValue = this.attributes.get(key);
        if (attributeValue == null) {
            return null;
        }

        return MineAttributeReflectionUtil.safeCast(key, attributeValue);
    }

    @Override
    public @NotNull <K, V> AttributeValue<V> computeIfAbsent(@NotNull AttributeKey<K, V> key, @NotNull V defaultValue) {
        return MineAttributeReflectionUtil.safeCast(
            key, this.attributes.compute(
                key, (k, v) -> Objects.requireNonNullElseGet(v, () -> AttributeValue.ofSafety(defaultValue, key.getValueType()))
            )
        );
    }

    @Override
    public <V> V getAttribute(@NotNull String identifier) {
        for (final Entry<AttributeKey<?, ?>, AttributeValue<?>> entry : this.attributes.entrySet()) {
            final AttributeKey<?, ?> key = entry.getKey();
            if (!key.getIdentifier().equals(identifier)) {
                continue;
            }

            final Object rawValue = entry.getValue().getValue();
            return (rawValue != null) ? MineAttributeReflectionUtil.safeValueCast(key, entry.getValue()) : null;
        }

        return null;
    }

    @Override
    @ThreadSafe
    public <K, V> @NotNull MineAttributeContainer withSafetyAttribute(@NotNull AttributeKey<K, V> key, @Nullable AttributeValue<V> value) {
        if (value == null || value.getValue() == null) {
            this.removeAttribute(key);
            return this;
        }

        final AttributeValue<V> attributeValue = MineAttributeReflectionUtil.safeCast(key, value);
        if (attributeValue == null) {
            this.removeAttribute(key);
            return this;
        }

        this.attributes.put(key, attributeValue);
        return this;
    }

    @Override
    public void removeAttribute(@NotNull AttributeKey<?, ?> key) {
        this.attributes.remove(key);
    }

    @Override
    public void clear() {
        this.attributes.clear();
    }
}