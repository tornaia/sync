package com.github.tornaia.sync.client.win;

import com.github.tornaia.sync.client.win.httpclient.RestHttpClient;
import com.github.tornaia.sync.client.win.statestorage.SyncStateManager;
import com.github.tornaia.sync.client.win.watchservice.DiskWatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.lang.management.ManagementFactory;

@SpringBootApplication
public class WinClientApp implements CommandLineRunner {

    @Autowired
    private RestHttpClient restHttpClient;

    @Autowired
    private SyncStateManager syncStateManager;

    @Autowired
    private DiskWatchService diskWatchService;

    @Override
    public void run(String... args) {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("PID: " + ManagementFactory.getRuntimeMXBean().getName());
        new SpringApplicationBuilder(WinClientApp.class).web(false).headless(false).run(args);

        Thread.sleep(Long.MAX_VALUE);
    }
}