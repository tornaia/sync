package com.github.tornaia.sync.e2e;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TwoClientsIntTest extends AbstractIntTest {

    private String userid = "100000";
    private Path client1Directory;
    private Path client2Directory;

    @Before
    public void initServerWith2Clients() throws Exception {
        startServer();
        resetDB();
        resetClientsFolder();

        client1Directory = startNewClient(userid);
        client2Directory = startNewClient(userid);
    }
    
    @Test
    public void bothCanStart() throws Exception {
        createFile(client1Directory.resolve("dummy.txt"), "dummy content", FileTime.fromMillis(500L), FileTime.fromMillis(600L));

        waitForSyncDone();

        assertTrue(client1Directory.toFile().list().length == 1);
        assertTrue(client2Directory.toFile().list().length == 1);
        assertEquals("dummy content", IOUtils.toString(new FileInputStream(client2Directory.resolve("dummy.txt").toFile())));
    }
}
