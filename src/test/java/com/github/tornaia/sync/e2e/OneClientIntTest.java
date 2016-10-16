package com.github.tornaia.sync.e2e;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

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

        assertThat(asList(client.syncDirectory.toFile().listFiles()),
                contains(new FileMatcher(client.syncDirectory)
                        .relativePath("dummy.txt")
                        .creationTime(500L)
                        .lastModifiedTime(600L)
                        .length(13)
                        .content("dummy content")));
    }
}
