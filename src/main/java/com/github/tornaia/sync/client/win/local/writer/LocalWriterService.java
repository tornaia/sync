package com.github.tornaia.sync.client.win.local.writer;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class LocalWriterService {

    @Value("${client.sync.directory.path:C:\\temp\\client}")
    private String syncDirectoryPath;

    @Autowired
    private DiskWriterService diskWriterService;

    private Path syncDirectory;

    @PostConstruct
    public void init() {
        syncDirectory = new File(syncDirectoryPath).toPath();
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
        return diskWriterService.replaceFileAtomically(tempFileWithRemoteContent.get(), localFileAbsolutePath);
    }

    public boolean delete(String relativePath) {
        Path localFileAbsolutePath = getAbsolutePath(relativePath);
        return diskWriterService.delete(localFileAbsolutePath);
    }

    private Path getAbsolutePath(String relativePath) {
        return syncDirectory.resolve(relativePath);
    }
}
