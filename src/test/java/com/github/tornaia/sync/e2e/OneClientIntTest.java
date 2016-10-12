package com.github.tornaia.sync.e2e;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OneClientIntTest extends AbstractIntTest {

    private String userid = "100001";

    @Test
    public void startOneClientThenCreateAFileThenCloseAndThenStart() throws Exception {
        AbstractIntTest.Client client = initClient(userid).start();
        createFile(client.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        waitForSyncDone();
        client.close();

        client.start();
        waitForSyncDone();

        assertTrue(client.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy content", IOUtils.toString(new FileInputStream(client.syncDirectory.resolve("dummy.txt").toFile())));
    }
}
