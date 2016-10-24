package com.github.tornaia.sync.client.win.local.writer;

import com.github.tornaia.sync.client.win.remote.RemoteKnownState;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Component
public class LocalWriterService {

    private static final Logger LOG = LoggerFactory.getLogger(LocalWriterService.class);

    @Value("${client.sync.userid}")
    private String userid;

    @Value("${client.sync.directory.path}")
    private String directoryPath;

    @Autowired
    private DiskWriterService diskWriterService;

    @Autowired
    private RemoteKnownState remoteKnownState;

    private Path syncDirectory;

    @PostConstruct
    public void init() {
        syncDirectory = new File(directoryPath).toPath();
    }

    public boolean write(FileMetaInfo fileMetaInfo, byte[] fileContent) {
        String relativePath = fileMetaInfo.relativePath;
        Path absolutePath = getAbsolutePath(relativePath);
        return diskWriterService.writeFileAtomically(absolutePath, fileContent, fileMetaInfo.creationDateTime, fileMetaInfo.modificationDateTime);
    }

    public boolean replace(String relativePath, FileMetaInfo remoteFileMetaInfo, byte[] remoteFileContent) {
        Path localFileAbsolutePath = getAbsolutePath(relativePath);
        Optional<Path> tempFileWithRemoteContent = diskWriterService.createTempFile(remoteFileContent, remoteFileMetaInfo.creationDateTime, remoteFileMetaInfo.modificationDateTime);
        if (!tempFileWithRemoteContent.isPresent()) {
            return false;
        }
        return diskWriterService.moveFileAtomically(tempFileWithRemoteContent.get(), localFileAbsolutePath);
    }

    public boolean delete(String relativePath) {
        Path localFileAbsolutePath = getAbsolutePath(relativePath);
        boolean isDirectory = localFileAbsolutePath.toFile().isDirectory();
        if (isDirectory) {
            boolean isDirectoryReadyForDeletion = true;
            // TODO use streams
            List<FileMetaInfo> itemsToDelete = remoteKnownState.getAllChildrenOrderedByPathLength(relativePath);
            for (FileMetaInfo itemToDelete : itemsToDelete) {
                File localFileToDelete = getAbsolutePath(itemToDelete.relativePath).toFile();
                try {
                    FileMetaInfo localFileMetaInfo = FileMetaInfo.createNonSyncedFileMetaInfo(userid, itemToDelete.relativePath, localFileToDelete);
                    boolean localCopyIsSynced = localFileMetaInfo.equals(itemToDelete);
                    if (localCopyIsSynced) {
                        diskWriterService.delete(localFileToDelete.toPath());
                    } else {
                        LOG.info("Unsynced file found under the directory: " + localFileMetaInfo);
                        isDirectoryReadyForDeletion = false;
                        break;
                    }
                } catch (IOException e) {
                    LOG.warn("Cannot delete file for some reason: " + e.getMessage());
                    isDirectoryReadyForDeletion = false;
                    break;
                }
            }
            if (!isDirectoryReadyForDeletion) {
                LOG.info("Directory wont be deleted. It has unsynced changes or locks: " + relativePath);
                return false;
            }
        }
        return diskWriterService.delete(localFileAbsolutePath);
    }

    private Path getAbsolutePath(String relativePath) {
        return syncDirectory.resolve(relativePath);
    }
}
