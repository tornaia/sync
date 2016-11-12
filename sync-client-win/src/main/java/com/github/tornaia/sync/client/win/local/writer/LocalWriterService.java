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

    public synchronized boolean write(FileMetaInfo fileMetaInfo, byte[] fileContent) {
        String relativePath = fileMetaInfo.relativePath;
        Path absolutePath = getAbsolutePath(relativePath);
        return diskWriterService.writeFileAtomically(absolutePath, fileContent, fileMetaInfo.creationDateTime, fileMetaInfo.modificationDateTime);
    }

    public synchronized boolean replace(String relativePath, FileMetaInfo remoteFileMetaInfo, byte[] remoteFileContent) {
        Path localFileAbsolutePath = getAbsolutePath(relativePath);
        Optional<Path> tempFileWithRemoteContent = diskWriterService.createTempFile(remoteFileContent, remoteFileMetaInfo.creationDateTime, remoteFileMetaInfo.modificationDateTime);
        if (!tempFileWithRemoteContent.isPresent()) {
            return false;
        }
        return diskWriterService.moveFileAtomically(tempFileWithRemoteContent.get(), localFileAbsolutePath);
    }

    public synchronized boolean delete(String relativePath) {
        Path localFileAbsolutePath = getAbsolutePath(relativePath);
        boolean isDirectory = localFileAbsolutePath.toFile().isDirectory();
        if (isDirectory) {
            List<FileMetaInfo> itemsUnderDirectory = remoteKnownState.getAllChildrenOrderedByPathLength(relativePath);
            if (!itemsUnderDirectory.isEmpty()) {
                LOG.warn("Wont delete a non-empty directory: " + relativePath + "(" + itemsUnderDirectory + ")");
                return false;
            }
        }
        return diskWriterService.delete(localFileAbsolutePath);
    }

    private Path getAbsolutePath(String relativePath) {
        return syncDirectory.resolve(relativePath);
    }
}
