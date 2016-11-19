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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

import static com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType.*;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DirectoryWatchServiceTest {

    private Path tempDirectory;

    private DirectoryWatchService directoryWatchService;

    private List<LocalFileCreatedEvent> localFileCreatedEvents = new ArrayList<>();
    private List<LocalFileModifiedEvent> localFileModifiedEvents = new ArrayList<>();
    private List<LocalFileDeletedEvent> localFileDeletedEvents = new ArrayList<>();

    @Before
    public void initListeners() throws Exception {
        tempDirectory = Files.createTempDirectory("junit_test");
        directoryWatchService = new DirectoryWatchService(tempDirectory.toFile().getAbsolutePath());
        directoryWatchService.addCreatedListener(item -> localFileCreatedEvents.add(item));
        directoryWatchService.addModifiedListener(item -> localFileModifiedEvents.add(item));
        directoryWatchService.addDeletedListener(item -> localFileDeletedEvents.add(item));
        directoryWatchService.start();
    }

    @After
    public void deleteTempDirectory() throws Exception {
        FileUtils.forceDelete(tempDirectory.toFile());
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

    @Test
    public void directoryCreated() throws Exception {
        createNewDirectory("directory");

        assertThat(localFileCreatedEvents, contains(event(CREATED, "directory")));
        assertTrue(localFileModifiedEvents.isEmpty());
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileDeleted() throws Exception {
        File newFile = createNewFile("file.txt");
        deleteFile(newFile);

        assertThat(localFileCreatedEvents, contains(event(CREATED, "file.txt")));
        assertThat(localFileModifiedEvents, contains(event(MODIFIED, "file.txt")));
        assertThat(localFileDeletedEvents, contains(event(DELETED, "file.txt")));
    }

    @Test
    public void directoryDeleted() throws Exception {
        File newDirectory = createNewDirectory("directory");
        deleteFile(newDirectory);

        assertThat(localFileDeletedEvents, contains(event(DELETED, "directory")));
    }

    @Test
    public void directoryWithSomeFilesDeleted() throws Exception {
        File directory = createNewDirectory("directory");
        createNewFile("directory/file1.txt");
        createNewFile("directory/file2.txt");
        deleteFile(directory);

        assertThat(localFileDeletedEvents, contains(event(DELETED, "directory\\file1.txt"), event(DELETED, "directory\\file2.txt"), event(DELETED, "directory")));
    }

    @Test
    public void fileDeletedWithinDirectory() throws Exception {
        createNewDirectory("directory");
        File file = createNewFile("directory/file.txt");
        deleteFile(file);

        assertThat(localFileDeletedEvents, contains(event(DELETED, "directory\\file.txt")));
    }

    @Test
    public void fileCreatedThenLocked() throws Exception {
        File file = createNewFile("locked-file");

        try (RandomAccessFile rwd = new RandomAccessFile(file, "rw")) {
            FileChannel channel = rwd.getChannel();
            channel.lock(0L, 0L, false);
        }

        waitForEvents();

        assertThat(localFileCreatedEvents, contains(event(CREATED, "locked-file")));
        assertTrue(localFileModifiedEvents.isEmpty());
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileCreatedThenLockedAndModified() throws Exception {
        File file = createNewFile("locked-file");

        try (RandomAccessFile rwd = new RandomAccessFile(file, "rw")) {
            FileChannel channel = rwd.getChannel();
            channel.lock(0L, 0L, false);

            try (FileWriter fw = new FileWriter(file)) {
                fw.append("newContent");
            }
        }

        waitForEvents();

        assertThat(localFileCreatedEvents, contains(event(CREATED, "locked-file")));
        assertThat(localFileModifiedEvents, contains(event(MODIFIED, "locked-file"), event(MODIFIED, "locked-file")));
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileCreatedThenLockedAndModifiedTwice() throws Exception {
        File file = createNewFile("locked-file");

        try (RandomAccessFile rwd = new RandomAccessFile(file, "rw")) {
            FileChannel channel = rwd.getChannel();
            channel.lock(0L, 0L, false);

            try (FileWriter fw = new FileWriter(file)) {
                fw.append("newContent");
                fw.append("newContent2");
            }
        }

        waitForEvents();

        assertThat(localFileCreatedEvents, contains(event(CREATED, "locked-file")));
        assertThat(localFileModifiedEvents, contains(event(MODIFIED, "locked-file"), event(MODIFIED, "locked-file")));
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    @Test
    public void fileStateReadWhileItWasInModificationSoAdditionalModifiedEventsAreFired() throws Exception {
        File file = createNewFile("locked-file");

        try (RandomAccessFile rwd = new RandomAccessFile(file, "rws")) {
            FileChannel channel = rwd.getChannel();
            channel.lock(0L, 0L, true);

            try (FileWriter fw = new FileWriter(file)) {
                fw.append("one");
                fw.flush();
                readFile(file);
                fw.append("two");
                fw.flush();
                readFile(file);
            }
        }

        waitForEvents();

        assertThat(localFileCreatedEvents, contains(event(CREATED, "locked-file")));
        assertThat(localFileModifiedEvents, hasItems(event(MODIFIED, "locked-file"), event(MODIFIED, "locked-file")));
        assertTrue(localFileDeletedEvents.isEmpty());
    }

    private File createNewFile(String fileName) throws Exception {
        File file = tempDirectory.resolve(fileName).toFile();
        boolean successfullyCreated = file.createNewFile();
        assertTrue(successfullyCreated);
        waitForEvents();
        return file;
    }

    private File createNewDirectory(String directoryName) throws Exception {
        File file = tempDirectory.resolve(directoryName).toFile();
        boolean successfullyCreated = file.mkdirs();
        assertTrue(successfullyCreated);
        waitForEvents();
        return file;
    }

    private String readFile(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            return IOUtils.toString(fis, Charset.defaultCharset());
        }
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

    private void deleteFile(File file) throws Exception {
        FileUtils.forceDelete(file);
        waitForEvents();
    }

    private void waitForEvents() throws Exception {
        Thread.sleep(100L);
    }

    private static LocalFileEventMatcher event(LocalFileEventType type, String relativePath) {
        return new LocalFileEventMatcher().eventType(type).relativePath(relativePath);
    }
}
