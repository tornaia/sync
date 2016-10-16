package com.github.tornaia.sync.e2e;

import org.junit.Ignore;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

public class TwoClientsIntTest extends AbstractIntTest {

    private String userid = "100002";

    @Test
    public void uglyFilenameWithUglyContentTest() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        String uglyFilename = "" + (char) 10000;
        createFile(client1.syncDirectory.resolve(uglyFilename), "\r", 500L, 600L);
        waitForSyncDone();

        // TODO one check is OK, and then a recursive check to verify that the two directories are equal
        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client1.syncDirectory)
                        .relativePath(uglyFilename)
                        .creationTime(500L)
                        .lastModifiedTime(600L)
                        .length(1)
                        .content("\r")));

        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client2.syncDirectory)
                        .relativePath(uglyFilename)
                        .creationTime(500L)
                        .lastModifiedTime(600L)
                        .length(1)
                        .content("\r")));
    }

    @Test
    public void startTwoClientsAndThenCreateOneFile() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client1.syncDirectory)
                        .relativePath("dummy.txt")
                        .creationTime(500L)
                        .lastModifiedTime(600L)
                        .length(13)
                        .content("dummy content")));

        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client2.syncDirectory)
                        .relativePath("dummy.txt")
                        .creationTime(500L)
                        .lastModifiedTime(600L)
                        .length(13)
                        .content("dummy content")));
    }

    @Test
    public void startOneClientThenCreateOneFileAndStartSecondClient() throws Exception {
        Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        waitForSyncDone();

        Client client2 = initClient(userid).start();
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client1.syncDirectory)
                        .relativePath("dummy.txt")
                        .creationTime(500L)
                        .lastModifiedTime(600L)
                        .length(13)
                        .content("dummy content")));

        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client2.syncDirectory)
                        .relativePath("dummy.txt")
                        .creationTime(500L)
                        .lastModifiedTime(600L)
                        .length(13)
                        .content("dummy content")));
    }

    @Test
    public void startOneClientThenCreateOneFileThenStopAndStartSecondClient() throws Exception {
        Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        waitForSyncDone();
        client1.close();

        Client client2 = initClient(userid).start();
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client1.syncDirectory)
                        .relativePath("dummy.txt")
                        .creationTime(500L)
                        .lastModifiedTime(600L)
                        .length(13)
                        .content("dummy content")));

        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client2.syncDirectory)
                        .relativePath("dummy.txt")
                        .creationTime(500L)
                        .lastModifiedTime(600L)
                        .length(13)
                        .content("dummy content")));
    }

    @Test
    public void createThenModifyFilesContentTest() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        waitForSyncDone();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content2", 700L, 800L);
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client1.syncDirectory)
                        .relativePath("dummy.txt")
                        .creationTime(700L)
                        .lastModifiedTime(800L)
                        .length(14)
                        .content("dummy content2")));

        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client2.syncDirectory)
                        .relativePath("dummy.txt")
                        .creationTime(700L)
                        .lastModifiedTime(800L)
                        .length(14)
                        .content("dummy content2")));
    }

    @Test
    public void bothClientsCreatesSameFileWithDifferentContentWhileTheyAreOffline() throws Exception {
        Client client1 = initClient(userid);
        Client client2 = initClient(userid);
        createFile(client1.syncDirectory.resolve("dummy.txt"), "1", 1476000000000L, 1476900000000L);
        createFile(client2.syncDirectory.resolve("dummy.txt"), "22", 1481400000000L, 1485000000000L);

        client1.start();
        waitForSyncDone();
        client2.start();
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(
                        new FileMatcher(client1.syncDirectory).relativePath("dummy.txt").creationTime(1476000000000L).lastModifiedTime(1476900000000L).length(1).content("1"),
                        new FileMatcher(client1.syncDirectory).relativePath("dummy_conflict_2_1481400000000_1485000000000.txt").creationTime(1481400000000L).lastModifiedTime(1485000000000L).length(2).content("22")
                ));
        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(
                        new FileMatcher(client2.syncDirectory).relativePath("dummy.txt").creationTime(1476000000000L).lastModifiedTime(1476900000000L).length(1).content("1"),
                        new FileMatcher(client2.syncDirectory).relativePath("dummy_conflict_2_1481400000000_1485000000000.txt").creationTime(1481400000000L).lastModifiedTime(1485000000000L).length(2).content("22")
                ));
    }

    @Test
    public void createOneFileThenDeleteIt() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("dummy.txt"), "dummy content", 500L, 600L);
        waitForSyncDone();
        deleteFile(client1.syncDirectory.resolve("dummy.txt"));
        waitForSyncDone();

        assertEquals(client1.syncDirectory.toFile().list().length, 0);
        assertEquals(client2.syncDirectory.toFile().list().length, 0);
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

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(
                        new FileMatcher(client1.syncDirectory).relativePath("dummy.txt").creationTime(1476000000000L).lastModifiedTime(1476900000000L).length(2).content("00"),
                        new FileMatcher(client1.syncDirectory).relativePath("dummy_conflict_2_10000_20000.txt").creationTime(10000L).lastModifiedTime(20000L).length(2).content("11"),
                        new FileMatcher(client1.syncDirectory).relativePath("dummy_conflict_2_30000_40000.txt").creationTime(30000L).lastModifiedTime(40000L).length(2).content("22")
                ));
        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(
                        new FileMatcher(client2.syncDirectory).relativePath("dummy.txt").creationTime(1476000000000L).lastModifiedTime(1476900000000L).length(2).content("00"),
                        new FileMatcher(client2.syncDirectory).relativePath("dummy_conflict_2_10000_20000.txt").creationTime(10000L).lastModifiedTime(20000L).length(2).content("11"),
                        new FileMatcher(client2.syncDirectory).relativePath("dummy_conflict_2_30000_40000.txt").creationTime(30000L).lastModifiedTime(40000L).length(2).content("22")
                ));
    }

    @Ignore("Not implemented yet")
    @Test
    public void emptyDirectory() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();
        createDirectory(client1.syncDirectory.resolve("emptyDirectory"));
        waitForSyncDone();

        assertTrue(client1.syncDirectory.toFile().list().length == 1);
        assertTrue(client2.syncDirectory.toFile().list().length == 1);
        assertTrue(client1.syncDirectory.resolve("emptyDirectory").toFile().isDirectory());
        assertTrue(client2.syncDirectory.resolve("emptyDirectory").toFile().isDirectory());
    }
}
