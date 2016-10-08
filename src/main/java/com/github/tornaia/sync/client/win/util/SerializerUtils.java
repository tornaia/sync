package com.github.tornaia.sync.client.win.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class SerializerUtils {

    public <T> T toObject(HttpEntity httpEntity, Class<T> clazz) {
        try {
            InputStream inputStream = httpEntity.getContent();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Cannot deserialize httpEntity content", e);
        }
    }

    public String toJSON(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize object", e);
        }
    }
}
