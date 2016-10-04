package com.github.tornaia.sync.e2e;

import com.github.tornaia.sync.client.win.WinClientApp;
import com.github.tornaia.sync.server.ServerApp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ServerApp.class, WinClientApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WinClientAndServerIntTests {

    private static final Logger LOG = LoggerFactory.getLogger(WinClientAndServerIntTests.class);

    @Autowired
    private ServerApp serverApp;

    @Autowired
    private WinClientApp winClientApp;

    @LocalServerPort
    private int serverPort;

    @Test
    public void bothCanStart() throws InterruptedException {
        LOG.info("Server.port: " + serverPort);
        assertNotNull(serverApp);
        assertNotNull(winClientApp);
        Thread.sleep(1000L);
        LOG.info("Now do your manipulations");
        Thread.sleep(5000L);
        LOG.info("Assert then over");
    }
}
