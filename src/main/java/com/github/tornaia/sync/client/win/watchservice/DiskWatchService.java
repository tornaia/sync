package com.github.tornaia.sync.client.win.watchservice;

import com.github.tornaia.sync.client.win.httpclient.RestHttpClient;
import com.github.tornaia.sync.client.win.httpclient.SyncResult;
import com.github.tornaia.sync.client.win.statestorage.SyncStateManager;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Objects;
import java.util.UUID;
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

    private Path syncDirectory;

    @PostConstruct
    public void startDiskWatch() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.syncDirectory = FileSystems.getDefault().getPath(SYNC_DIRECTORY_PATH);

        register(syncDirectory);
        registerChildrenRecursively(syncDirectory);

        new Thread(() -> runInBackground()).start();
    }

    public void writeToDisk(FileMetaInfo fileMetaInfo, byte[] content) {
        Path tempFile;
        try {
            tempFile = Files.createTempFile(UUID.randomUUID().toString(), "suffix");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            IOUtils.write(content, fos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Files.setAttribute(tempFile, "basic:lastModifiedTime", FileTime.fromMillis(fileMetaInfo.modificationDateTime));
            Files.setAttribute(tempFile, "basic:creationTime", FileTime.fromMillis(fileMetaInfo.creationDateTime));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO C:\temp + \5.txt -> C:\5.txt -> so substring(1) quick ugly hack
        Path targetFile = syncDirectory.resolve(fileMetaInfo.relativePath.substring(1));
        syncStateManager.onFileModify(fileMetaInfo);
        try {
            targetFile.toFile().getParentFile().mkdirs();
            Files.move(tempFile, targetFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            FileMetaInfo newFileMetaInfo = FileMetaInfo.createNonSyncedFileMetaInfo(relativePath, file);
            FileMetaInfo isMaybeKnown = syncStateManager.getFileMetaInfo(newFileMetaInfo.relativePath);
            if (Objects.equals(isMaybeKnown, newFileMetaInfo)) {
                // nothing to do
                return;
            }

            SyncResult syncResult = restHttpClient.onFileCreate(newFileMetaInfo, file);
            if (syncResult.status == SyncResult.Status.TRANSFER_FAILED) {
                // the app cannot read the file, maybe its in use, or removed during upload, network problem, etc..
                // more sophisticated logic needed here
                return;
            }

            syncStateManager.onFileModify(syncResult.fileMetaInfo);
            if (syncResult.status == SyncResult.Status.CONFLICT) {
                handleConflict(newFileMetaInfo);
            }
        } else if (file.isDirectory()) {
            registerChildrenRecursively(filePath.toAbsolutePath());
        } else {
            System.out.println("Unknown file type. File does not exist? " + file);
        }
    }

    private void handleConflict(FileMetaInfo fileMetaInfo) {
        Path fileToRename = syncDirectory.resolve(fileMetaInfo.relativePath);
        try {
            FileUtils.moveFile(fileToRename.toFile(), new File(fileToRename.toFile().getAbsolutePath() + "_conflict" + System.currentTimeMillis()));
            downloadOtherFile(fileMetaInfo.relativePath);
        } catch (IOException e) {
            System.out.println("Cannot rename file: " + e);
        }
    }

    private void downloadOtherFile(String relativePath) {
        // TODO lokális fájlt átnevezni, aztán a szerverről a másikat letölteni jól
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

            FileMetaInfo updatedFileMetaInfo = new FileMetaInfo(fileMetaInfo.id, relativePath, file);
            boolean fileIsInASyncedStateSoItsADummyEventOnlySoWeCanIgnoreIt = Objects.equals(updatedFileMetaInfo, fileMetaInfo);
            if (fileIsInASyncedStateSoItsADummyEventOnlySoWeCanIgnoreIt) {
                return;
            }

            SyncResult syncResult = restHttpClient.onFileModify(updatedFileMetaInfo, file);
            if (syncResult.status == SyncResult.Status.CONFLICT) {
                handleConflict(fileMetaInfo);
                return;
            }

            syncStateManager.onFileModify(syncResult.fileMetaInfo);
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
        boolean clientDoesNotKnowAnythingAboutThisFile = Objects.isNull(fileMetaInfo);
        if (clientDoesNotKnowAnythingAboutThisFile) {
            return;
        }
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
