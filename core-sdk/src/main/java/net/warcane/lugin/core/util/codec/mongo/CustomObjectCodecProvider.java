package net.warcane.lugin.core.util.codec.mongo;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

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
}