package com.github.tornaia.sync.shared.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class SerializerUtils {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SerializerUtils() {
        objectMapper.getFactory().configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
    }

    public <T> T toObject(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Cannot deserialize string content", e);
        }
    }

    public <T> T toObject(InputStream inputStream, Class<T> clazz) {
        try {
            String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return toObject(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Cannot deserialize inputStream content", e);
        }
    }

    public <T> T toObject(HttpEntity httpEntity, Class<T> clazz) {
        try {
            InputStream inputStream = httpEntity.getContent();
            return toObject(inputStream, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Cannot deserialize httpEntity content", e);
        }
    }

    public String toJSON(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize object", e);
        }
    }
}
