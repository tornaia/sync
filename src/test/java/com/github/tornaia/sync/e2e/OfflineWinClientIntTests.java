package com.github.tornaia.sync.e2e;

import com.github.tornaia.sync.client.win.WinClientApp;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WinClientApp.class, webEnvironment = WebEnvironment.NONE)
public class OfflineWinClientIntTests {

    @Autowired
    private WinClientApp winClientApp;

    @Test
    public void canStartInOffline() {
        assertNotNull(winClientApp);
    }
}
