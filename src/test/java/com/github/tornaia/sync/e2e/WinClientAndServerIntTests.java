package com.github.tornaia.sync.e2e;

import com.github.tornaia.sync.client.win.WinClientApp;
import com.github.tornaia.sync.server.ServerApp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WinClientAndServerIntTests {

    private static final Logger LOG = LoggerFactory.getLogger(WinClientAndServerIntTests.class);

    @Before
    public void initServerWith2Clients() {
        new SpringApplicationBuilder(ServerApp.class).headless(false).run();

        new SpringApplicationBuilder(WinClientApp.class)
                .web(false)
                .headless(false)
                .run("--client.sync.directory.path=C:\\temp\\client-1\\",
                        "--client.state.file.path=C:\\temp2\\client-1.db");

        new SpringApplicationBuilder(WinClientApp.class)
                .web(false)
                .headless(false)
                .run("--client.sync.directory.path=C:\\temp\\client-2\\",
                        "--client.state.file.path=C:\\temp2\\client-2.db");
    }

    @Test
    public void bothCanStart() throws InterruptedException {
        LOG.info("Do manipulate");
        Thread.sleep(5000L);
        LOG.info("Do assert");
    }
}
