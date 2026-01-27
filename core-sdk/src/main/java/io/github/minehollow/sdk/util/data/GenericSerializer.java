package io.github.minehollow.sdk.util.data;

import io.github.minehollow.sdk.util.JsonUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Representa um serializador genérico que pode ser usado para serializar throwable desserializar objetos de qualquer tipo.
 *
 * @param <O> O tipo do objeto a ser serializado/deserializado
 */
public class GenericSerializer<O> {

    public static <O> GenericSerializer<O> jsonSerializer(@NotNull Class<O> typeClazz) {
        return new GenericSerializer<>(
          JsonUtil::toJson,
          data -> JsonUtil.fromJson(data, typeClazz)
        );
    }

    private final Function<O, String> deserializer;
    private final Function<String, O> serializer;

    public GenericSerializer(Function<O, String> deserializer, Function<String, O> serializer) {
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    public O deserialize(String data) {
        return serializer.apply(data);
    }

    public String serialize(O object) {
        return deserializer.apply(object);
    }
}
