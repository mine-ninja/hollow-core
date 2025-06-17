package net.warcane.lugin.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

@Slf4j
public class JsonUtil {

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    public static final ObjectMapper YAML_MAPPER = new ObjectMapper()
      .enable(INDENT_OUTPUT);


    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return JSON_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            log.error("Erro ao converter JSON para objeto: {}", e.getMessage(), e);
            return null;
        }
    }


    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        try {
            return JSON_MAPPER.readValue(json, JSON_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("Erro ao converter JSON para lista: {}", e.getMessage(), e);
            return null;
        }
    }

    public static String toJson(Object object) {
        try {
            return JSON_MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            log.error("Erro ao converter objeto para JSON: {}", e.getMessage(), e);
            return null;
        }
    }
}
