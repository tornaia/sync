package com.github.tornaia.sync.client.win.local.reader;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Value("${client.sync.directory.path:C:\\temp\\client\\}")
    private String syncDirectoryPath;

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    private final List<LocalFileEvent> events = new ArrayList<>();

    private WatchService watchService;

    private Path syncDirectory;

    @PostConstruct
    public void startDiskWatch() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.syncDirectory = FileSystems.getDefault().getPath(syncDirectoryPath);
        Files.createDirectories(syncDirectory);
        register(syncDirectory);
        registerChildrenRecursively(syncDirectory);

        Thread thread = new Thread(() -> runInBackground());
        thread.setDaemon(true);
        thread.start();
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
                    onFileCreate(filePath);
                } else if (kind == ENTRY_MODIFY) {
                    onFileModify(filePath);
                } else if (kind == ENTRY_DELETE) {
                    onFileDelete(filePath);
                }

                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

            Thread.yield();
        }
    }

    private void onFileCreate(Path filePath) {
        File file = filePath.toFile();
        String relativePath = getRelativePath(file);

        if (file.isFile()) {
            addNewEvent(new FileCreatedEvent(relativePath));
        } else if (file.isDirectory()) {
            registerChildrenRecursively(filePath.toAbsolutePath());
        } else {
            LOG.info("Creation event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    private void onFileModify(Path filePath) {
        File file = filePath.toFile();
        if (file.isDirectory()) {
            return;
        } else if (file.isFile()) {
            String relativePath = getRelativePath(file);
            addNewEvent(new FileModifiedEvent(relativePath));
        } else {
            LOG.info("Modification event of an unknown file type. File/directory does not exist? " + file);
        }
    }

    private void onFileDelete(Path filePath) {
        File file = filePath.toFile();
        if (file.exists()) {
            LOG.info("Delete event of a non-existing file: " + file);
            return;
        }

        String relativePath = getRelativePath(file);
        addNewEvent(new FileDeleteEvent(relativePath));
    }

    private void register(Path syncDirectory) {
        try {
            syncDirectory.register(watchService, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW});
        } catch (IOException e) {
            LOG.warn("Cannot register watcher: " + e);
        }
    }

    private void registerChildrenRecursively(Path root) {
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

    // always without leading slash
    private String getRelativePath(File directory) {
        return directory.getAbsolutePath().substring(new File(syncDirectoryPath).getAbsolutePath().length() + 1);
    }

    private Path getAbsolutePath(String relativePath) {
        return syncDirectory.resolve(relativePath);
    }
}
