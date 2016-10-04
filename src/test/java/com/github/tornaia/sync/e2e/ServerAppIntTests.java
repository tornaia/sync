package com.github.tornaia.sync.e2e;

import com.github.tornaia.sync.server.ServerApp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ServerApp.class)
public class ServerAppIntTests {

    @Autowired
    private ServerApp serverApp;

    @Test
    public void serverCanStart() {
        assertNotNull(serverApp);
    }
}
