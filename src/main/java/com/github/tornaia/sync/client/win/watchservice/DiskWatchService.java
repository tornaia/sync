package com.github.tornaia.sync.client.win.watchservice;

import com.github.tornaia.sync.client.win.httpclient.FileCreateResponse;
import com.github.tornaia.sync.client.win.httpclient.RestHttpClient;
import com.github.tornaia.sync.client.win.statestorage.SyncStateManager;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger LOG = LoggerFactory.getLogger(DiskWatchService.class);

    @Value("${client.sync.directory.path:C:\\temp\\client\\}")
    private String syncDirectoryPath;

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    @Autowired
    private SyncStateManager syncStateManager;

    @Autowired
    private RestHttpClient restHttpClient;

    private WatchService watchService;

    private Path syncDirectory;

    @PostConstruct
    public void startDiskWatch() throws IOException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.syncDirectory = FileSystems.getDefault().getPath(syncDirectoryPath);
        Files.createDirectories(syncDirectory);
        register(syncDirectory);
        registerChildrenRecursively(syncDirectory);

        // TODO ugly solution
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

    public boolean isFileOnDisk(FileMetaInfo fileMetaInfo) {
        Path fileOnDiskPath = syncDirectory.resolve(fileMetaInfo.relativePath.substring(1));
        File fileOnDisk = fileOnDiskPath.toFile();
        if (!fileOnDisk.exists()) {
            return false;
        }

        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(fileOnDisk.toPath(), BasicFileAttributes.class);
        } catch (IOException e) {
            LOG.warn("Cannot access to file: " + fileOnDisk.getAbsolutePath());
            return false;
        }
        long length = attr.size();
        long creationDateTime = attr.creationTime().toMillis();
        long modificationDateTime = attr.lastModifiedTime().toMillis();

        if (Objects.equals(fileMetaInfo.length, length) && Objects.equals(fileMetaInfo.creationDateTime, creationDateTime) && Objects.equals(fileMetaInfo.modificationDateTime, modificationDateTime)) {
            return true;
        }

        return false;

    }

    private void onFileCreate(Path filePath) {
        File file = filePath.toFile();
        String relativePath = getRelativePath(file);

        if (file.isFile()) {
            FileMetaInfo newFileMetaInfo = FileMetaInfo.createNonSyncedFileMetaInfo(userid, relativePath, file);
            FileMetaInfo isMaybeKnown = syncStateManager.getFileMetaInfo(newFileMetaInfo.relativePath);
            if (Objects.equals(isMaybeKnown, newFileMetaInfo)) {
                // nothing to do
                return;
            }

            FileCreateResponse fileCreateResponse = restHttpClient.onFileCreate(newFileMetaInfo, file);
            if (fileCreateResponse.status == FileCreateResponse.Status.TRANSFER_FAILED) {
                // the app cannot read the file, maybe its in use, or removed during upload, network problem, etc..
                // more sophisticated logic needed here
                return;
            }

            syncStateManager.onFileModify(fileCreateResponse.fileMetaInfo);
            if (fileCreateResponse.status == FileCreateResponse.Status.CONFLICT) {
                handleConflict(newFileMetaInfo);
            }
        } else if (file.isDirectory()) {
            registerChildrenRecursively(filePath.toAbsolutePath());
        } else {
            LOG.info("Unknown file type. File does not exist? " + file);
        }
    }

    private void handleConflict(FileMetaInfo fileMetaInfo) {
        Path fileToRename = syncDirectory.resolve(fileMetaInfo.relativePath);
        try {
            FileUtils.moveFile(fileToRename.toFile(), new File(fileToRename.toFile().getAbsolutePath() + "_conflict" + System.currentTimeMillis()));
            downloadOtherFile(fileMetaInfo.relativePath);
        } catch (IOException e) {
            LOG.warn("Cannot rename file: " + e);
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

            FileMetaInfo updatedFileMetaInfo = new FileMetaInfo(fileMetaInfo.id, userid, relativePath, file);
            boolean fileIsInASyncedStateSoItsADummyEventOnlySoWeCanIgnoreIt = Objects.equals(updatedFileMetaInfo, fileMetaInfo);
            if (fileIsInASyncedStateSoItsADummyEventOnlySoWeCanIgnoreIt) {
                return;
            }

            FileCreateResponse fileCreateResponse = restHttpClient.onFileModify(updatedFileMetaInfo, file);
            if (fileCreateResponse.status == FileCreateResponse.Status.CONFLICT) {
                handleConflict(fileMetaInfo);
                return;
            }

            syncStateManager.onFileModify(fileCreateResponse.fileMetaInfo);
        } else {
            LOG.info("Unknown file type. File does not exist? " + file);
        }
    }

    private void onFileDelete(Path filePath) {
        File file = filePath.toFile();
        if (file.exists()) {
            LOG.info("File exist when we want to sync a delete event? " + file);
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

    private String getRelativePath(File directory) {
        return directory.getAbsolutePath().substring(new File(syncDirectoryPath).getAbsolutePath().length());
    }
}
