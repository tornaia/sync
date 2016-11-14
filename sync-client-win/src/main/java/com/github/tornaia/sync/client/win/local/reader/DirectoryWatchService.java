package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileCreatedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileDeletedEvent;
import com.github.tornaia.sync.client.win.local.reader.event.single.LocalFileModifiedEvent;
import com.sun.nio.file.ExtendedWatchEventModifier;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;
import static java.nio.file.StandardWatchEventKinds.*;

public class DirectoryWatchService {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryWatchService.class);

    private Set<Consumer<LocalFileCreatedEvent>> createdListeners = new LinkedHashSet<>();
    private Set<Consumer<LocalFileModifiedEvent>> modifiedListeners = new LinkedHashSet<>();
    private Set<Consumer<LocalFileDeletedEvent>> deletedListeners = new LinkedHashSet<>();

    private Path syncDirectory;

    private WatchService watchService;

    private Thread directoryEventsReaderThread;

    private volatile boolean contextIsRunning;

    public DirectoryWatchService(String syncDirectoryAsString) throws IOException {
        FileSystem fileSystem = FileSystems.getDefault();
        this.syncDirectory = fileSystem.getPath(syncDirectoryAsString);
        Files.createDirectories(syncDirectory);
    }

    public synchronized void addCreatedListener(Consumer<LocalFileCreatedEvent> fileCreatedConsumer) {
        createdListeners.add(fileCreatedConsumer);
    }

    public synchronized void addModifiedListener(Consumer<LocalFileModifiedEvent> fileModifiedConsumer) {
        modifiedListeners.add(fileModifiedConsumer);
    }

    public synchronized void addDeletedListener(Consumer<LocalFileDeletedEvent> fileDeletedConsumer) {
        deletedListeners.add(fileDeletedConsumer);
    }

    public synchronized void start() {
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new IllegalStateException("Should not happen", e);
        }

        registerWatcherAndAddAllFiles();

        directoryEventsReaderThread = new Thread(() -> readAndDispatchEvents());
        directoryEventsReaderThread.setDaemon(true);
        directoryEventsReaderThread.setName("DirectoryWatchService");
        directoryEventsReaderThread.start();
    }

    public synchronized void stop() {
        contextIsRunning = false;
        try {
            watchService.close();
        } catch (IOException e) {
            throw new IllegalStateException("Should not happen", e);
        }
        directoryEventsReaderThread.interrupt();
    }

    public synchronized void restart() {
        stop();
        start();
    }

    private void registerWatcherAndAddAllFiles() {
        try {
            WatchEvent.Kind[] kinds = {ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW};
            WatchEvent.Modifier[] modifiers = new WatchEvent.Modifier[]{ExtendedWatchEventModifier.FILE_TREE/*, SensitivityWatchEventModifier.HIGH*/};
            syncDirectory.register(watchService, kinds, modifiers);
        } catch (IOException e) {
            throw new IllegalStateException("Should not happen", e);
        }

        Path rootDirectoryPath = syncDirectory.toAbsolutePath();
        onFileCreated(rootDirectoryPath);
    }

    private void readAndDispatchEvents() {
        contextIsRunning = true;

        while (contextIsRunning) {
            WatchKey key;
            try {
                key = watchService.poll(25, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ClosedWatchServiceException e) {
                return;
            }
            if (key == null) {
                Thread.yield();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();

                if (kind == OVERFLOW) {
                    LOG.warn("Overflow! Restarting DirectoryWatchService");
                    restart();
                    return;
                }

                Path ownerPath = (Path) key.watchable();
                Path filePath = ownerPath.resolve(filename);
                if (kind == ENTRY_CREATE) {
                    onFileCreated(filePath);
                } else if (kind == ENTRY_MODIFY) {
                    onFileModified(filePath);
                } else if (kind == ENTRY_DELETE) {
                    onFileDeleted(filePath);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

            Thread.yield();
        }
    }

    private void onFileCreated(Path filePath) {
        File file = filePath.toFile();
        String relativePath = getRelativePath(file);

        boolean isDirectory = file.isDirectory();
        if (isDirectory) {
            boolean isRootDirectory = filePath.equals(syncDirectory);
            if (!isRootDirectory) {
                createdListeners.stream().forEach(listener -> listener.accept(new LocalFileCreatedEvent(relativePath)));
            }
            Set<LocalFileCreatedEvent> createdEvents = getCreatedEventsForEachFile(filePath);
            createdEvents.stream().forEach(event -> createdListeners.stream().forEach(listener -> listener.accept(event)));
        } else {
            createdListeners.stream().forEach(listener -> listener.accept(new LocalFileCreatedEvent(relativePath)));
        }
    }

    private void onFileModified(Path filePath) {
        File file = filePath.toFile();
        String relativePath = getRelativePath(file);
        modifiedListeners.stream().forEach(listener -> listener.accept(new LocalFileModifiedEvent(relativePath)));
    }

    private void onFileDeleted(Path filePath) {
        File file = filePath.toFile();
        String relativePath = getRelativePath(file);
        deletedListeners.stream().forEach(listener -> listener.accept(new LocalFileDeletedEvent(relativePath)));
    }

    private Set<LocalFileCreatedEvent> getCreatedEventsForEachFile(Path root) {
        Set<LocalFileCreatedEvent> createdFileEvents = new HashSet<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (dir.equals(root)) {
                        return FileVisitResult.CONTINUE;
                    }

                    String relativePath = getRelativePath(dir.toFile());
                    createdFileEvents.add(new LocalFileCreatedEvent(relativePath));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = getRelativePath(file.toFile());
                    createdFileEvents.add(new LocalFileCreatedEvent(relativePath));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (FileSystemException e) {
            throw new IllegalStateException("TODO, Cannot scan filesystem: ", e);
        } catch (IOException e) {
            throw new IllegalStateException("TODO, Cannot scan filesystem: ", e);
        }

        return createdFileEvents;
    }

    private String getRelativePath(File file) {
        return syncDirectory.relativize(file.toPath()).toString();
    }
}
