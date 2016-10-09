package com.github.tornaia.sync.e2e;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.nio.file.attribute.FileTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TwoClientsIntTest extends AbstractIntTest {

    private String userid = "100000";

    @Before
    public void initServerWith2Clients() throws Exception {
        startServer();
        resetDB();
    }

    @Test
    public void startTwoClientsAndThenCreateOneFile() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", FileTime.fromMillis(500L), FileTime.fromMillis(600L));
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy content", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy.txt").toFile())));
    }

    @Test
    public void startOneClientThenCreateOneFileAndStartSecondClient() throws Exception {
        Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy2.txt"), "dummy2 content", FileTime.fromMillis(500L), FileTime.fromMillis(600L));
        waitForSyncDone();

        Client client2 = initClient(userid).start();
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy2 content", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy2.txt").toFile())));
    }

    @Test
    public void startOneClientThenCreateOneFileThenStopAndStartSecondClient() throws Exception {
        Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy2.txt"), "dummy2 content", FileTime.fromMillis(500L), FileTime.fromMillis(600L));
        waitForSyncDone();
        client1.stop();

        Client client2 = initClient(userid).start();
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy2 content", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy2.txt").toFile())));
    }

    @Test
    public void startOneClientThenCreateAFileThenStopAndThenStart() throws Exception {
        Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy2.txt"), "dummy2 content", FileTime.fromMillis(500L), FileTime.fromMillis(600L));
        waitForSyncDone();
        client1.stop();

        client1.start();
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy2 content", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy2.txt").toFile())));
    }
}
