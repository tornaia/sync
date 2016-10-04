package com.github.tornaia.sync.client.win;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.lang.management.ManagementFactory;

@SpringBootApplication
public class WinClientApp implements CommandLineRunner {

    @Override
    public void run(String... args) {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("PID: " + ManagementFactory.getRuntimeMXBean().getName());
        new SpringApplicationBuilder(WinClientApp.class).web(false).headless(false).run(args);
        Thread.sleep(Long.MAX_VALUE);
    }
}