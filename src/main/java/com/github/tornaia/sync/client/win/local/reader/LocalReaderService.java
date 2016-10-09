package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.client.win.util.FileUtils;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
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

    private final List<LocalFileEvent> events = new ArrayList<>();

    private WatchService watchService;

    private Path syncDirectory;

    @PostConstruct
    public void startDiskWatch() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.syncDirectory = FileSystems.getDefault().getPath(syncDirectoryPath);
        Files.createDirectories(syncDirectory);
        register(syncDirectory);
        registerChildrenDirectoriesRecursively(syncDirectory);

        Thread thread = new Thread(() -> runInBackground());
        thread.setDaemon(true);
        thread.setName(userid + "-" + syncDirectoryPath.substring(syncDirectoryPath.length() - 1) + "-LocalR");
        thread.start();

        addAllLocalFilesToChangeList(syncDirectory);
    }

    private void addAllLocalFilesToChangeList(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
                    LOG.warn("Cannot visit directory", e);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
                    LOG.warn("Cannot visit file", e);
                    return FileVisitResult.CONTINUE;
                }

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

    public synchronized Optional<LocalFileEvent> getNext() {
        if (events.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(events.remove(0));
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

    private synchronized void addNewEvent(LocalFileEvent localFileEvent) {
        // TODO later here we can combine events to optimize things like:
        // create-delete (same path) -> nothing
        // create-delete-create (same path) -> last create only
        events.add(localFileEvent);
    }

    private void runInBackground() {
        while (true) {
            WatchKey key;
            try {
                key = watchService.poll(25, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
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
                    LOG.info("WatchService registered for dir: " + dir.toFile().getAbsolutePath());
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
