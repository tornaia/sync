package com.github.tornaia.sync.e2e;

import org.junit.Test;
import org.springframework.test.annotation.Repeat;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.*;

public class TwoClientsIntTest extends AbstractIntTest {

    private String userid = "100002";

    @Test
    @Repeat(REPEAT)
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
    @Repeat(REPEAT)
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
    @Repeat(REPEAT)
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
    @Repeat(REPEAT)
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
    @Repeat(REPEAT)
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
    @Repeat(REPEAT)
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
    @Repeat(REPEAT)
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
    @Repeat(REPEAT)
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

    @Test
    @Repeat(REPEAT)
    public void emptyDirectory() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();

        createDirectory(client1.syncDirectory.resolve("emptyDirectory"), 1476000000000L, 1476900000000L);
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(
                        new DirectoryMatcher(client1.syncDirectory).relativePath("emptyDirectory").creationTime(1476000000000L).lastModifiedTime(1476900000000L)
                ));
        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(
                        new DirectoryMatcher(client2.syncDirectory).relativePath("emptyDirectory").creationTime(1476000000000L)
                ));
    }

    @Test
    @Repeat(REPEAT)
    public void emptyDirectoryDelete() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();

        createDirectory(client1.syncDirectory.resolve("emptyDirectory"), 1476000000000L, 1476900000000L);
        waitForSyncDone();

        deleteDirectory(client1.syncDirectory.resolve("emptyDirectory"));
        waitForSyncDone();

        assertTrue(asList(client1.syncDirectory.toFile().listFiles()).isEmpty());
        assertTrue(asList(client2.syncDirectory.toFile().listFiles()).isEmpty());
    }

    @Test
    @Repeat(REPEAT)
    public void createEmptyDirectoryWhenClientIsOffline() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid);

        createDirectory(client2.syncDirectory.resolve("emptyDirectory"), 1476000000000L, 1476900000000L);
        client2.start();
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(
                        // TODO add lastModifiedRecently check to these checks
                        new DirectoryMatcher(client1.syncDirectory).relativePath("emptyDirectory").creationTime(1476000000000L)
                ));
        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(
                        new DirectoryMatcher(client2.syncDirectory).relativePath("emptyDirectory").creationTime(1476000000000L)
                ));
    }

    @Test
    @Repeat(REPEAT)
    public void directoryCreationTimeAndLastModifiedTimeNotSynced() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid);

        createDirectory(client1.syncDirectory.resolve("emptyDirectory"), 1476000000000L, 1476900000000L);
        createDirectory(client2.syncDirectory.resolve("emptyDirectory"), 100L, 200L);
        client2.start();
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(
                        new DirectoryMatcher(client1.syncDirectory).relativePath("emptyDirectory").creationTime(1476000000000L).lastModifiedTime(1476900000000L)
                ));
        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(
                        new DirectoryMatcher(client2.syncDirectory).relativePath("emptyDirectory").creationTime(100L).lastModifiedTime(200L)
                ));
    }

    @Test
    @Repeat(REPEAT)
    public void directoryAttributesAreNotSynced() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();

        createDirectory(client1.syncDirectory.resolve("emptyDirectory"), 100L, 200L);
        waitForSyncDone();
        setCreationTimeAndModifiedTime(client1.syncDirectory.resolve("emptyDirectory"), 300000L, 400000L);
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(
                        new DirectoryMatcher(client1.syncDirectory).relativePath("emptyDirectory").creationTime(300000L)
                ));
        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(
                        new DirectoryMatcher(client2.syncDirectory).relativePath("emptyDirectory").creationTime(100L)
                ));
    }

    @Test
    @Repeat(REPEAT)
    public void directoryWithContentDelete() throws Exception {
        Client client1 = initClient(userid).start();
        Client client2 = initClient(userid).start();

        createFile(client1.syncDirectory.resolve("dir").resolve("subdir").resolve("file1.txt"), "whatever1", 100L, 200L);
        createFile(client1.syncDirectory.resolve("dir").resolve("subdir").resolve("file2.txt"), "whatever2", 300L, 400L);
        createFile(client1.syncDirectory.resolve("dir").resolve("subdir").resolve("file3.txt"), "whatever3", 500L, 600L);
        waitForSyncDone();

        deleteDirectory(client1.syncDirectory.resolve("dir"));
        waitForSyncDone();

        assertTrue(asList(client1.syncDirectory.toFile().listFiles()).isEmpty());
        assertTrue(asList(client2.syncDirectory.toFile().listFiles()).isEmpty());
    }

    @Test
    @Repeat(REPEAT)
    public void directoryAndFileNameEqualsSoConflict() throws Exception {
        Client client1 = initClient(userid).start();
        createFile(client1.syncDirectory.resolve("file-directory"), "whatever1", 100L, 200L);

        Client client2 = initClient(userid);
        createDirectory(client2.syncDirectory.resolve("file-directory"), 300L, 400L);
        client2.start();
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(
                        new FileMatcher(client1.syncDirectory).relativePath("file-directory").content("whatever1").creationTime(100L).lastModifiedTime(200L),
                        new DirectoryMatcher(client1.syncDirectory).relativePath("file-directory_conflict_0_300_400")
                ));
        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(
                        new FileMatcher(client2.syncDirectory).relativePath("file-directory").content("whatever1").creationTime(100L).lastModifiedTime(200L),
                        new DirectoryMatcher(client2.syncDirectory).relativePath("file-directory_conflict_0_300_400")
                ));
    }

    @Test
    @Repeat(REPEAT)
    public void directoryAndFileNameEqualsSoConflictOtherWay() throws Exception {
        Client client1 = initClient(userid).start();
        createDirectory(client1.syncDirectory.resolve("file-directory"), 100L, 200L);

        Client client2 = initClient(userid);
        createFile(client2.syncDirectory.resolve("file-directory"), "whatever1", 300L, 400L);
        client2.start();
        waitForSyncDone();

        assertThat(asList(client1.syncDirectory.toFile().listFiles()),
                contains(
                        new DirectoryMatcher(client1.syncDirectory).relativePath("file-directory").creationTime(100L).lastModifiedTime(200L),
                        new FileMatcher(client1.syncDirectory).relativePath("file-directory_conflict_9_300_400").content("whatever1").creationTime(300L).lastModifiedTime(400L)
                ));
        assertThat(asList(client2.syncDirectory.toFile().listFiles()),
                contains(
                        new DirectoryMatcher(client2.syncDirectory).relativePath("file-directory").creationTime(100L),
                        new FileMatcher(client2.syncDirectory).relativePath("file-directory_conflict_9_300_400").content("whatever1").creationTime(300L).lastModifiedTime(400L)
                ));
    }
}
