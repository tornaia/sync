package com.github.tornaia.sync.e2e;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class TwoClientsIntTest extends AbstractIntTest {

    @Parameterized.Parameters
    public static List<Object[]> data() {
        return Arrays.asList(new Object[1][0]);
    }

    private String userid = "100002";

    @Before
    public void initServerWith2Clients() throws Exception {
        startServer();
        resetDB();
    }

    @Test
    public void uglyFilenameWithUglyContentTest() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        String uglyFilename = "" + (char) 10000;
        createFile(client1.syncDirectory.resolve(uglyFilename), "\r", 500L, 600L);
        waitForSyncDone();
        stopClients();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("\r", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve(uglyFilename).toFile())));
        assertEquals("\r", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve(uglyFilename).toFile())));
    }

    @Test
    public void startTwoClientsAndThenCreateOneFile() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        waitForSyncDone();
        stopClients();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy content", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy.txt").toFile())));
        assertEquals("dummy content", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy.txt").toFile())));
    }

    @Test
    public void startOneClientThenCreateOneFileAndStartSecondClient() throws Exception {
        Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy2.txt"), "dummy2 content", 500L, 600L);
        waitForSyncDone();

        Client client2 = initClient(userid).start();
        waitForSyncDone();
        stopClients();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy2 content", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy2.txt").toFile())));
    }

    @Test
    public void startOneClientThenCreateOneFileThenStopAndStartSecondClient() throws Exception {
        Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy2.txt"), "dummy2 content", 500L, 600L);
        waitForSyncDone();
        client1.stop();

        Client client2 = initClient(userid).start();
        waitForSyncDone();
        stopClients();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy2 content", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy2.txt").toFile())));
        assertEquals("dummy2 content", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy2.txt").toFile())));
    }

    @Test
    public void createThenModifyFilesContentTest() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content2", 700L, 800L);
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy content2", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy.txt").toFile())));
        assertEquals("dummy content2", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy.txt").toFile())));
    }

    @Test
    public void createThenModifyFilesContentInAnotherClientTest() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        waitForSyncDone();
        createFile(client2.syncDirectory.resolve("dummy.txt"), "dummy content2", 700L, 800L);
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy content2", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy.txt").toFile())));
        assertEquals("dummy content2", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy.txt").toFile())));
    }

    // @Repeat(1000)
    @Test
    public void bothClientsCreatesSameFileWithDifferentContentWhileTheyAreOffline() throws Exception {
        Client client1 = initClient(userid);
        Client client2 = initClient(userid);
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content1", 1476000000000L, 1476900000000L);
        createFile(client2.syncDirectory.resolve("dummy.txt"), "dummy content2", 1485000000000L, 1481400000000L);

        client1.start();
        waitForSyncDone();
        client2.start();
        waitForSyncDone();
        stopClients();

        assertTrue(client1.syncDirectory.toFile().list().length == 2);
        assertTrue(client2.syncDirectory.toFile().list().length == 2);
        assertEquals("dummy content1", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy.txt").toFile())));
        assertEquals("dummy content1", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy.txt").toFile())));
        assertEquals("dummy content2", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy_conflict_14_1485000000000_1481400000000.txt").toFile())));
        assertEquals("dummy content2", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy_conflict_14_1485000000000_1481400000000.txt").toFile())));
    }
}
