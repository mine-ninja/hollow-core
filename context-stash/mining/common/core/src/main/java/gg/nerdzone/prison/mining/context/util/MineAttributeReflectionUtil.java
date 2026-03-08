/*
 * Copyright (c) 2025.
 *
 * Authored by the Nerdzone Team: https://github.com/orgs/nerdzonegg
 */

package gg.nerdzone.prison.mining.context.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gg.nerdzone.prison.mining.api.context.MineAttributeContainer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for handling attribute reflection and casting. This class provides methods to safely cast attribute values. It uses MethodHandles to perform
 * type-safe checks and casts, ensuring that the attribute values are of the expected type defined by the AttributeKey. Caching is used to optimize the
 * performance of the reflection operations and resource usage using a temporary cache thread-safety to avoid memory leaks.
 */
@SuppressWarnings("unchecked")
@UtilityClass
public final class MineAttributeReflectionUtil {

    private final Cache<Class<?>, MethodHandle> INSTANCE_CACHE = CacheBuilder.newBuilder()
        .expireAfterAccess(10, TimeUnit.MINUTES)
        .maximumSize(512) // Adjust the maximum size as needed (mem vs. performance)
        .build();

    private final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

    /**
     * Performs a type-safe cast of an attribute value using the expected type from the {@code AttributeKey}.
     *
     * @param key   the attribute key that defines the expected type
     * @param value the value a tribute value to cast
     * @param <V>   value type
     * @return raw value of the attribute value
     */
    public <V> V safeValueCast(MineAttributeContainer.AttributeKey<?, ?> key, MineAttributeContainer.AttributeValue<?> value) {
        final MineAttributeContainer.AttributeValue<?> attributeValue = safeCast(key, value);
        if (attributeValue == null) {
            return null;
        }

        return (V) value.getValue();
    }

    /**
     * Performs a type-safe cast of an attribute value using the expected type from the {@code AttributeKey}.
     *
     * @param key   the attribute key that defines the expected type
     * @param value the value attribute value to cast
     * @param <K>   key type
     * @param <V>   value type
     * @return the cast AttributeValue or null if raw value is null
     * @throws IllegalArgumentException if the value inside the AttributeValue does not match the key type
     */
    public <K, V> MineAttributeContainer.AttributeValue<V> safeCast(MineAttributeContainer.AttributeKey<K, V> key, MineAttributeContainer.AttributeValue<?> value) {
        if (value == null) {
            return null;
        }

        if (!isInstance(key, value)) {
            return null;
        }

        return (MineAttributeContainer.AttributeValue<V>) value;
    }

    /**
     * Validates if the internal value of an AttributeValue matches the expected value type from the key.
     */
    public boolean isInstance(MineAttributeContainer.AttributeKey<?, ?> key, MineAttributeContainer.AttributeValue<?> value) {
        if (value == null || value.getValue() == null) {
            return false;
        }

        try {
            final MethodHandle methodHandle = INSTANCE_CACHE.get(key.getValueType(), () -> createIsInstanceHandle(key.getValueType()));
            return (boolean) methodHandle.invoke(value.getValue());
        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to invoke isInstance MethodHandle for key: " + key.getIdentifier(), throwable);
        }
    }

    @ApiStatus.Internal
    private @NotNull MethodHandle createIsInstanceHandle(@NonNull Class<?> type) {
        try {
            return LOOKUP.findVirtual(
                Class.class, "isInstance",
                MethodType.methodType(boolean.class, Object.class)
            ).bindTo(type);
        } catch (Throwable throwable) {
            throw new RuntimeException("Unable to bind isInstance for type: " + type.getName(), throwable);
        }
    }
}
