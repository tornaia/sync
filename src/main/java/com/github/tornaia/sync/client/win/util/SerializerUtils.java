package com.github.tornaia.sync.client.win.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;

import java.io.IOException;
import java.io.InputStream;

public final class SerializerUtils {

    private SerializerUtils() {
    }

    public static <T> T toObject(HttpEntity httpEntity, Class<T> clazz) {
        try {
            InputStream inputStream = httpEntity.getContent();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Cannot deserialize httpEntity content", e);
        }
    }
}
