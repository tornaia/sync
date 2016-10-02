package com.github.tornaia.sync.client.win.watchservice;

import com.github.tornaia.sync.client.win.httpclient.RestHttpClient;
import com.github.tornaia.sync.client.win.statestorage.SyncStateManager;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

@Component
public class DiskWatchService {

    private static final String SYNC_DIRECTORY_PATH = "C:\\temp\\";

    @Autowired
    private SyncStateManager syncStateManager;

    @Autowired
    private RestHttpClient restHttpClient;

    private WatchService watchService;

    @PostConstruct
    public void startDiskWatch() throws IOException {
        watchService = FileSystems.getDefault().newWatchService();

        Path syncDirectory = FileSystems.getDefault().getPath(SYNC_DIRECTORY_PATH);
        register(syncDirectory);
        registerChildrenRecursively(syncDirectory);

        new Thread(() -> runInBackground()).start();
    }

    public void runInBackground() {
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
            FileMetaInfo fileMetaInfo = FileMetaInfo.createNonSyncedFileMetaInfo(relativePath, file);
            restHttpClient.onFileCreate(fileMetaInfo, file);
        } else if (file.isDirectory()) {
            registerChildrenRecursively(filePath.toAbsolutePath());
        } else {
            System.out.println("Unknown file type. File does not exist? " + file);
        }
    }

    private void onFileModify(Path filePath) {
        File file = filePath.toFile();
        if (file.isDirectory()) {
            return;
        } else if (file.isFile()) {
            String relativePath = getRelativePath(file);
            FileMetaInfo fileMetaInfo = syncStateManager.getFileMetaInfo(relativePath);
            boolean fromServerPointOfViewItIsANewFile = Objects.isNull(fileMetaInfo);
            if (fromServerPointOfViewItIsANewFile) {
                onFileCreate(filePath);
                return;
            }
            restHttpClient.onFileModify(fileMetaInfo, file);
            syncStateManager.onFileModify(fileMetaInfo);
        } else {
            System.out.println("Unknown file type. File does not exist? " + file);
        }
    }

    private void onFileDelete(Path filePath) {
        File file = filePath.toFile();
        if (file.exists()) {
            System.out.println("File exist when we want to sync a delete event? " + file);
            return;
        }
        String relativePath = getRelativePath(file);
        FileMetaInfo fileMetaInfo = syncStateManager.getFileMetaInfo(relativePath);
        restHttpClient.onFileDelete(fileMetaInfo);
        syncStateManager.onFileDelete(relativePath);
    }

    private void register(Path syncDirectory) {
        try {
            syncDirectory.register(watchService, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW});
        } catch (IOException e) {
            System.out.println("Cannot register watcher: " + e);
        }
    }

    private void registerChildrenRecursively(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    System.out.println("WatchService registered for dir: " + dir.getFileName());
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    System.out.println("VISIT FAILED: " + file + ", exception: " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getRelativePath(File directory) {
        return directory.getAbsolutePath().substring(new File(SYNC_DIRECTORY_PATH).getAbsolutePath().length());
    }
}
