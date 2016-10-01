package com.github.tornaia.sync.client.win.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;

public final class SerializerUtils {

    private SerializerUtils() {
    }

    public static StringEntity toStringEntity(Object obj) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            String jsonInString = mapper.writeValueAsString(obj);
            return new StringEntity(jsonInString);
        } catch (JsonProcessingException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
