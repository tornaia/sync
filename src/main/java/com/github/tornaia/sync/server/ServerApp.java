package com.github.tornaia.sync.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

import java.lang.management.ManagementFactory;

import static org.springframework.boot.SpringApplication.run;

@EnableWebSocket
@SpringBootApplication
public class ServerApp {

    private static final Logger LOG = LoggerFactory.getLogger(ServerApp.class);

    public static void main(String[] args) {
        LOG.info("PID: " + ManagementFactory.getRuntimeMXBean().getName());
        run(ServerApp.class, args);
    }
}
