package com.github.tornaia.sync.server.hello;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class HelloController {

    private AtomicInteger counter = new AtomicInteger();

    @RequestMapping("/api/hello")
    public HelloResponse hello() {
        return new HelloResponse("Hello " + counter.getAndIncrement());
    }
}
