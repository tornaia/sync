package com.github.tornaia.sync.client.win.util;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RandomUtils {

    public String getRandomString() {
        return UUID.randomUUID().toString();
    }
}
