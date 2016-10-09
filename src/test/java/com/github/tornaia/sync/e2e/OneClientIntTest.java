package com.github.tornaia.sync.e2e;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.FileInputStream;
import java.nio.file.attribute.FileTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OneClientIntTest extends AbstractIntTest {

    private String userid = "100001";

    @Test
    public void startOneClientThenCreateAFileThenStopAndThenStart() throws Exception {
        AbstractIntTest.Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy2.txt"), "dummy2 content", 500L, 600L);
        waitForSyncDone();
        client1.stop();

        client1.start();
        waitForSyncDone();
        stopClients();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy2 content", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy2.txt").toFile())));
    }
}
