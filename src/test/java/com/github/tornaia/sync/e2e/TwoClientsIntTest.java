package com.github.tornaia.sync.e2e;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.FileInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TwoClientsIntTest extends AbstractIntTest {

    private String userid = "100002";

    @Test
    public void uglyFilenameWithUglyContentTest() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        String uglyFilename = "" + (char) 10000;
        createFile(client1.syncDirectory.resolve(uglyFilename), "\r", 500L, 600L);
        waitForSyncDone();

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

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertEquals("dummy2 content", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy2.txt").toFile())));
    }

    @Test
    public void startOneClientThenCreateOneFileThenStopAndStartSecondClient() throws Exception {
        Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy2.txt"), "dummy2 content", 500L, 600L);
        waitForSyncDone();
        client1.close();

        Client client2 = initClient(userid).start();
        waitForSyncDone();

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

    @Test
    public void bothClientsCreatesSameFileWithDifferentContentWhileTheyAreOffline() throws Exception {
        Client client1 = initClient(userid);
        Client client2 = initClient(userid);
        createFile(client1.syncDirectory.resolve("dummy.txt"), "1", 1476000000000L, 1476900000000L);
        createFile(client2.syncDirectory.resolve("dummy.txt"), "22", 1485000000000L, 1481400000000L);

        client1.start();
        waitForSyncDone();
        client2.start();
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 2);
        assertTrue(client2.syncDirectory.toFile().list().length == 2);
        assertEquals("1", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy.txt").toFile())));
        assertEquals("1", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy.txt").toFile())));
        assertEquals("22", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy_conflict_2_1485000000000_1481400000000.txt").toFile())));
        assertEquals("22", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy_conflict_2_1485000000000_1481400000000.txt").toFile())));
    }

    @Test
    public void createOneFileThenDeleteIt() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        waitForSyncDone();
        deleteFile(client1.syncDirectory.resolve("dummy.txt"));
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 0);
        assertTrue(client2.syncDirectory.toFile().list().length == 0);
    }


    @Test
    public void modifyOfflineInBothClients() throws Exception {
        Client client1 = initClient(userid);
        Client client2 = initClient(userid);
        createFile(client1.syncDirectory.resolve("dummy.txt"), "00", 1476000000000L, 1476900000000L);

        client1.start();
        client2.start();
        waitForSyncDone();
        client1.close();
        client2.close();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "11", 10000L, 20000L);
        createFile(client2.syncDirectory.resolve("dummy.txt"), "22", 30000L, 40000L);
        client1.start();
        waitForSyncDone();
        client2.start();
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 3);
        assertTrue(client2.syncDirectory.toFile().list().length == 3);
        assertEquals("00", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy.txt").toFile())));
        assertEquals("00", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy.txt").toFile())));
        assertEquals("11", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy_conflict_2_10000_20000.txt").toFile())));
        assertEquals("11", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy_conflict_2_10000_20000.txt").toFile())));
        assertEquals("22", IOUtils.toString(new FileInputStream(client1.syncDirectory.resolve("dummy_conflict_2_30000_40000.txt").toFile())));
        assertEquals("22", IOUtils.toString(new FileInputStream(client2.syncDirectory.resolve("dummy_conflict_2_30000_40000.txt").toFile())));
    }
}
