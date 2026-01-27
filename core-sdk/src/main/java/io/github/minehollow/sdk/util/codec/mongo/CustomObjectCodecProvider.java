package io.github.minehollow.sdk.util.codec.mongo;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomObjectCodecProvider implements CodecProvider {
    private final Codec<Object> objectCodec;

    public CustomObjectCodecProvider() {
        this.objectCodec = new ObjectCodec();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz == Object.class) {
            return (Codec<T>) objectCodec;
        }
        return null;
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, List<Type> typeArguments, CodecRegistry registry) {
        if (Map.class.isAssignableFrom(clazz) && typeArguments != null && typeArguments.size() >= 2) {
            Type keyType = typeArguments.get(0);
            if (keyType == UUID.class) {
                Type valueType = typeArguments.get(1);
                Codec<?> valueCodec = resolveValueCodec(valueType, registry);
                return (Codec<T>) new UuidMapCodec<>((Codec<Object>) valueCodec);
            }
        }
        if (clazz == Object.class) {
            return (Codec<T>) objectCodec;
        }
        return null;
    }
    private Codec<?> resolveValueCodec(Type valueType, CodecRegistry registry) {
        if (valueType instanceof Class<?>) {
            return registry.get((Class<?>) valueType);
        }
        // Fallback for parameterized/wildcard types
        return objectCodec;
    }
}
