package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.client.win.util.FileUtils;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

@Component
public class LocalReaderService {

    private static final Logger LOG = LoggerFactory.getLogger(LocalReaderService.class);

    @Value("${client.sync.directory.path:C:\\temp\\client}")
    private String syncDirectoryPath;

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    @Autowired
    private FileUtils fileUtils;

    private List<LocalFileEvent> events = new ArrayList<>();

    private WatchService watchService;

    private Path syncDirectory;

    private volatile boolean contextIsRunning;

    @EventListener({ContextRefreshedEvent.class})
    public void ContextRefreshedEvent() throws IOException {
        LOG.info("Context refreshed event happened");
        contextIsRunning = true;
        startDiskWatch();
    }

    @EventListener({ContextClosedEvent.class})
    public void onContextClosedEvent() throws IOException {
        LOG.info("Context closed event happened");
        contextIsRunning = false;
        watchService.close();
    }

    public void startDiskWatch() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.syncDirectory = FileSystems.getDefault().getPath(syncDirectoryPath);
        Files.createDirectories(syncDirectory);

        Thread directoryEventsReaderThread = new Thread(() -> runInBackground());
        directoryEventsReaderThread.setDaemon(true);
        directoryEventsReaderThread.setName(userid + "-" + syncDirectoryPath.substring(syncDirectoryPath.length() - 1) + "-LocalR");
        directoryEventsReaderThread.start();

        Thread directoryWatcherRegisterThread = new Thread(() -> runDirWatcherRegisterInBackground());
        directoryWatcherRegisterThread.setDaemon(true);
        directoryWatcherRegisterThread.setName(userid + "-" + syncDirectoryPath.substring(syncDirectoryPath.length() - 1) + "-DirWR");
        directoryWatcherRegisterThread.start();
    }

    private void runDirWatcherRegisterInBackground() {
        while (contextIsRunning) {
            LOG.debug("Rescan directory tree");
            register(syncDirectory);
            registerChildrenDirectoriesRecursively(syncDirectory);
            addAllLocalFilesToChangeList(syncDirectory);
            try {
                Thread.sleep(30000L);
            } catch (InterruptedException ie) {
                LOG.warn("Run terminated: " + ie.getMessage());
            }
        }
    }

    public Optional<LocalFileEvent> getNextCreated() {
        synchronized (this) {
            Optional<LocalFileEvent> first = events.stream()
                    .filter(e -> Objects.equals(LocalEventType.CREATED, e.eventType))
                    .findFirst();
            if (first.isPresent()) {
                events.remove(first.get());
            }
            return first;
        }
    }

    public Optional<LocalFileEvent> getNextModified() {
        synchronized (this) {
            Optional<LocalFileEvent> first = events.stream()
                    .filter(e -> Objects.equals(LocalEventType.MODIFIED, e.eventType))
                    .findFirst();
            if (first.isPresent()) {
                events.remove(first.get());
            }
            return first;
        }
    }

    public Optional<LocalFileEvent> getNextDeleted() {
        synchronized (this) {
            Optional<LocalFileEvent> first = events.stream()
                    .filter(e -> Objects.equals(LocalEventType.DELETED, e.eventType))
                    .findFirst();
            if (first.isPresent()) {
                events.remove(first.get());
            }
            return first;
        }
    }

    public Optional<FileMetaInfo> getFileMetaInfo(String relativePath) {
        Path absolutePath = getAbsolutePath(relativePath);
        try {
            if (!exists(relativePath)) {
                return Optional.empty();
            }
            File file = absolutePath.toFile();
            FileMetaInfo fileMetaInfo = new FileMetaInfo(null, userid, relativePath, file);
            return Optional.of(fileMetaInfo);
        } catch (IOException e) {
            LOG.warn("Cannot read file from disk! Does not exist or locked?", e);
            return Optional.empty();
        }
    }

    public boolean exists(String relativePath) {
        Path absolutePath = getAbsolutePath(relativePath);
        return absolutePath.toFile().exists();
    }

    public void reAddEvent(LocalFileEvent localFileEvent) {
        boolean isFileExist = syncDirectory.resolve(localFileEvent.relativePath).toFile().exists();
        boolean isDelete = Objects.equals(localFileEvent.eventType, LocalEventType.DELETED);
        if (isDelete && isFileExist) {
            LOG.warn("Does not make sense to re-add DELETED local event to LocalReaderService when the file is on the disk: " + localFileEvent.relativePath);
            return;
        }

        if (isFileExist) {
            LOG.debug("External hint to re-add event: " + localFileEvent);
            addNewEvent(localFileEvent);
        } else {
            LOG.warn("Cannot re-add event since file does not exist: " + localFileEvent);
        }
    }

    private void addNewEvent(LocalFileEvent localFileEvent) {
        // TODO later here we can combine events to optimize things like:
        // create-delete (same path) -> nothing
        // create-delete-create (same path) -> last create only
        synchronized (this) {
            events.add(localFileEvent);
        }
    }

    private void addAllLocalFilesToChangeList(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = getRelativePath(file.toFile());
                    LOG.trace("Visit file: " + relativePath);
                    // TODO it is not a FileCreatedEvent but more a FileAddedEvent
                    addNewEvent(new FileCreatedEvent(relativePath));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void runInBackground() {
        while (contextIsRunning) {
            WatchKey key;
            try {
                key = watchService.poll(25, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ClosedWatchServiceException e) {
                LOG.warn("Run terminated: " + e.getMessage());
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
                    continue;
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

        if (file.isFile()) {
            LOG.info("File created event: " + fileUtils.getDescriptionForFile(file));
            addNewEvent(new FileCreatedEvent(relativePath));
        } else if (file.isDirectory()) {
            registerChildrenDirectoriesRecursively(filePath.toAbsolutePath());
        } else {
            LOG.info("Created event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    private void onFileModified(Path filePath) {
        File file = filePath.toFile();
        if (file.isDirectory()) {
            return;
        } else if (file.isFile()) {
            String relativePath = getRelativePath(file);
            LOG.info("File modified event: " + fileUtils.getDescriptionForFile(file));
            addNewEvent(new FileModifiedEvent(relativePath));
        } else {
            LOG.info("Modified event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    private void onFileDeleted(Path filePath) {
        File file = filePath.toFile();
        if (file.exists()) {
            LOG.info("Deleted event of a non-existing file: " + file);
            return;
        }

        String relativePath = getRelativePath(file);
        LOG.info("File deleted event: " + relativePath);
        addNewEvent(new FileDeleteEvent(relativePath));
    }

    private void register(Path syncDirectory) {
        try {
            syncDirectory.register(watchService, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW});
        } catch (IOException e) {
            LOG.warn("Cannot register watcher: " + e);
        }
    }

    private void registerChildrenDirectoriesRecursively(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    LOG.debug("WatchService registered for dir: " + dir.toFile().getAbsolutePath());
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    LOG.warn("VISIT FAILED: " + file + ", exception: " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getRelativePath(File file) {
        return syncDirectory.relativize(file.toPath()).toString();
    }

    private Path getAbsolutePath(String relativePath) {
        return syncDirectory.resolve(relativePath);
    }
}
