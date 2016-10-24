package com.github.tornaia.sync.e2e;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class HundredClientsIntTest extends AbstractIntTest {

    private static final int NUM_OF_CLIENTS = 100;

    private String userid = "100100";

    @Test
    public void fileSyncedTo100Clients() throws Exception {
        List<Client> clients = IntStream.range(0, NUM_OF_CLIENTS)
                .parallel()
                .mapToObj(i -> initClient(userid))
                .map(client -> client.start())
                .collect(Collectors.toList());

        Client client0 = clients.get(0);
        createFile(client0.syncDirectory.resolve("filename.txt"), "content", 500L, 600L);
        waitForSyncDone();
        waitForSyncDone();
        waitForSyncDone();
        waitForSyncDone();
        waitForSyncDone();

        clients.stream().forEach(client -> {
            assertThat(asList(client.syncDirectory.toFile().listFiles()),
                    contains(new FileMatcher(client.syncDirectory)
                            .relativePath("filename.txt")
                            .creationTime(500L)
                            .lastModifiedTime(600L)
                            .size(7L)
                            .content("content")));
        });
    }
}
