package net.warcane.lugin.core.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

@Slf4j
public class JsonUtil {

    public static final ObjectMapper JSON_MAPPER = JsonMapper.builder()
      .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
      .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
      .configure(MapperFeature.AUTO_DETECT_SETTERS, false)
      .configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true)
      .serializationInclusion(JsonInclude.Include.NON_NULL)
      .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
      .addModules(new RecordNamingStrategyPatchModule(), new JavaTimeModule(), new ParameterNamesModule())
      .build();


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

    @NotNull
    public static String toJson(Object object) {
        try {
            return JSON_MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao converter objeto para JSON: " + e.getMessage(), e);
        }
    }

    public static byte[] toJsonBytes(Object object) {
        try {
            return JSON_MAPPER.writeValueAsBytes(object);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao converter objeto para JSON bytes: " + e.getMessage(), e);
        }
    }

    @NotNull
    public static <T> T fromJson(byte[] data, Class<T> clazz) throws IOException {
        return JSON_MAPPER.readValue(data, clazz);
    }
}
