package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileCreatedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileDeletedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileEventMatcher;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileModifiedEvent;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType.CREATED;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DirectoryWatchServiceTest {

    private Path tempDirectory = Files.createTempDirectory("junit_test");

    private DirectoryWatchService directoryWatchService = new DirectoryWatchService(tempDirectory.toFile().getAbsolutePath());

    private List<LocalFileCreatedEvent> localFileCreatedEvents = new ArrayList<>();
    private List<LocalFileModifiedEvent> localFileModifiedEvents = new ArrayList<>();
    private List<LocalFileDeletedEvent> localFileDeletedEvents = new ArrayList<>();

    public DirectoryWatchServiceTest() throws Exception {
    }

    @Before
    public void initListeners() throws Exception {
        directoryWatchService.addCreatedListener(item -> localFileCreatedEvents.add(item));
        directoryWatchService.addModifiedListener(item -> localFileModifiedEvents.add(item));
        directoryWatchService.addDeletedListener(item -> localFileDeletedEvents.add(item));
        directoryWatchService.start();
    }

    @After
    public void deleteTempDirectory() throws Exception {
        FileUtils.deleteDirectory(tempDirectory.toFile());
    }

    @Test
    public void noEventsByDefault() throws Exception {
        assertTrue(localFileCreatedEvents.isEmpty());
        assertTrue(localFileModifiedEvents.isEmpty());
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileCreated() throws Exception {
        tempDirectory.resolve("file.txt").toFile().createNewFile();

        waitForEvents();

        assertThat(localFileCreatedEvents, hasItems(new LocalFileEventMatcher().eventType(CREATED).relativePath("file.txt")));
        assertTrue(localFileModifiedEvents.isEmpty());
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void filesCreated() throws Exception {
        tempDirectory.resolve("file1.txt").toFile().createNewFile();
        tempDirectory.resolve("file2.txt").toFile().createNewFile();

        waitForEvents();

        assertThat(localFileCreatedEvents, hasItems(new LocalFileEventMatcher().eventType(CREATED).relativePath("file1.txt"), new LocalFileEventMatcher().eventType(CREATED).relativePath("file2.txt")));
        assertTrue(localFileModifiedEvents.isEmpty());
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    private void waitForEvents() throws Exception {
        Thread.sleep(50L);
    }
}
