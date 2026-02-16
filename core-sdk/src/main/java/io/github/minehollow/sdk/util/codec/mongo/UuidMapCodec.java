package io.github.minehollow.sdk.util.codec.mongo;

import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class UuidMapCodec<T> implements Codec<Map<UUID, T>> {
    private final Codec<T> valueCodec;

    public UuidMapCodec(Codec<T> codec) {
        this.valueCodec = codec;
    }

    @Override
    public Map<UUID, T> decode(BsonReader reader, DecoderContext decoderContext) {
        Map<UUID, T> map = new HashMap<>();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String key = reader.readName();
            T value = decoderContext.decodeWithChildContext(this.valueCodec, reader);
            map.put(UUID.fromString(key), value);
        }
        reader.readEndDocument();
        return map;
    }


    @Override
    public void encode(BsonWriter writer, Map<UUID, T> value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        for (var entry : value.entrySet()) {
            writer.writeName(entry.getKey().toString());
            encoderContext.encodeWithChildContext(this.valueCodec, writer, entry.getValue());
        }
        writer.writeEndDocument();
    }

    @Override
    public Class<Map<UUID, T>> getEncoderClass() {
        @SuppressWarnings("unchecked")
        Class<Map<UUID, T>> clazz = (Class<Map<UUID, T>>) (Class<?>) Map.class;
        return clazz;
    }

}
