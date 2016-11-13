package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileCreatedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileDeletedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileEventMatcher;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileModifiedEvent;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import static com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType.CREATED;
import static com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType.MODIFIED;
import static org.hamcrest.Matchers.contains;
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
        createNewFile("file.txt");

        assertThat(localFileCreatedEvents, contains(event(CREATED, "file.txt")));
        assertTrue(localFileModifiedEvents.isEmpty());
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void filesCreated() throws Exception {
        createNewFile("file1.txt");
        createNewFile("file2.txt");

        assertThat(localFileCreatedEvents, contains(event(CREATED, "file1.txt"), event(CREATED, "file2.txt")));
        assertTrue(localFileModifiedEvents.isEmpty());
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileContentModified() throws Exception {
        File file = createNewFile("file.txt");
        modifyContent(file, "newContent");

        assertThat(localFileCreatedEvents, contains(event(CREATED, "file.txt")));
        assertThat(localFileModifiedEvents, contains(event(MODIFIED, "file.txt"), event(MODIFIED, "file.txt")));
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileContentModifiedTwiceToTheSameContent() throws Exception {
        File file = createNewFile("file.txt");
        modifyContent(file, "newContent");
        modifyContent(file, "newContent");

        assertThat(localFileCreatedEvents, contains(event(CREATED, "file.txt")));
        assertThat(localFileModifiedEvents, contains(event(MODIFIED, "file.txt"), event(MODIFIED, "file.txt"), event(MODIFIED, "file.txt"), event(MODIFIED, "file.txt")));
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileCreationTimeModified() throws Exception {
        File file = createNewFile("file.txt");
        setCreationTime(file, 100L);

        assertThat(localFileCreatedEvents, contains(event(CREATED, "file.txt")));
        assertThat(localFileModifiedEvents, contains(event(MODIFIED, "file.txt")));
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileCreationTimeModifiedTwice() throws Exception {
        File file = createNewFile("file.txt");
        setCreationTime(file, 100L);
        setCreationTime(file, 200L);

        assertThat(localFileCreatedEvents, contains(event(CREATED, "file.txt")));
        assertThat(localFileModifiedEvents, contains(event(MODIFIED, "file.txt"), event(MODIFIED, "file.txt")));
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileCreationTimeModifiedTwiceToTheSameValue() throws Exception {
        File file = createNewFile("file.txt");
        setCreationTime(file, 100L);
        setCreationTime(file, 100L);

        assertThat(localFileCreatedEvents, contains(event(CREATED, "file.txt")));
        assertThat(localFileModifiedEvents, contains(event(MODIFIED, "file.txt"), event(MODIFIED, "file.txt")));
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    private File createNewFile(String filename) throws Exception {
        File file = tempDirectory.resolve(filename).toFile();
        boolean successfullyCreated = file.createNewFile();
        assertTrue(successfullyCreated);
        waitForEvents();
        return file;
    }

    private void setCreationTime(File file, long creationTime) throws Exception {
        Files.setAttribute(file.toPath(), "basic:creationTime", FileTime.fromMillis(creationTime));
        waitForEvents();
    }

    private void modifyContent(File file, String newContent) throws Exception {
        try (FileWriter fw = new FileWriter(file)) {
            IOUtils.write(newContent, fw);
        }
        waitForEvents();
    }

    private void waitForEvents() throws Exception {
        Thread.sleep(50L);
    }

    private static LocalFileEventMatcher event(LocalFileEventType type, String relativePath) {
        return new LocalFileEventMatcher().eventType(type).relativePath(relativePath);
    }
}
