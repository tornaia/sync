package com.github.tornaia.sync.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class SerializerUtils {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public <T> T toObject(HttpEntity httpEntity, Class<T> clazz) {
        try {
            InputStream inputStream = httpEntity.getContent();
            return objectMapper.readValue(inputStream, clazz);
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