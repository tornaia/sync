package com.github.tornaia.sync.client.win.remote.writer;

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
import java.util.Objects;

@Component
public class RemoteWriterService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteWriterService.class);

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    @Value("${client.sync.directory.path:C:\\temp\\client\\}")
    private String syncDirectoryPath;

    @Autowired
    private RemoteRestCommandService remoteRestCommandService;

    private Path syncDirectory;

    @PostConstruct
    public void init() {
        syncDirectory = new File(syncDirectoryPath).toPath();
    }

    public boolean createFile(String relativePath) {
        Path absolutePath = getAbsolutePath(relativePath);
        File file = absolutePath.toFile();

        FileMetaInfo fileMetaInfo;
        try {
            fileMetaInfo = FileMetaInfo.createNonSyncedFileMetaInfo(userid, relativePath, file);
        } catch (IOException e) {
            LOG.error("Cannot read file", e);
            return false;
        }

        FileCreateResponse fileCreateResponse = remoteRestCommandService.onFileCreate(fileMetaInfo, file);
        return Objects.equals(FileCreateResponse.Status.OK, fileCreateResponse.status);
    }

    private Path getAbsolutePath(String relativePath) {
        String relativePathWithoutLeadingSlash = relativePath;
        return syncDirectory.resolve(relativePathWithoutLeadingSlash);
    }
}
