package com.github.tornaia.sync.client.win.remote.writer;

import com.github.tornaia.sync.client.win.local.writer.DiskWriterService;
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

    @Autowired
    private DiskWriterService diskWriterService;

    @Autowired
    private RemoteKnownState remoteKnownState;

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

        if (remoteKnownState.isKnown(fileMetaInfo)) {
            LOG.info("File is already known by server: " + fileMetaInfo);
            return true;
        }

        FileCreateResponse fileCreateResponse = remoteRestCommandService.onFileCreate(fileMetaInfo, file);
        boolean ok = Objects.equals(FileCreateResponse.Status.OK, fileCreateResponse.status);
        if (ok) {
            LOG.info("File uploaded to server: " + fileCreateResponse.fileMetaInfo);
            return true;
        }

        boolean conflict = Objects.equals(FileCreateResponse.Status.CONFLICT, fileCreateResponse.status);
        if (conflict) {
            // TODO move this conflict file name creation to a separate object
            String originalFileName = absolutePath.toFile().getAbsolutePath();
            boolean hasExtension = originalFileName.indexOf('.') != -1;
            String postFix = "_conflict_" + fileMetaInfo.length + "_" + fileMetaInfo.creationDateTime + "_" + fileMetaInfo.modificationDateTime;
            String conflictFileName = hasExtension ? originalFileName.split("\\.", 2)[0] + postFix + "." + originalFileName.split("\\.", 2)[1] : originalFileName + postFix;
            // TODO what should happen when this renamed/conflictFileName file exists?
            Path renamed = new File(absolutePath.toFile().getParentFile().getAbsolutePath()).toPath().resolve(conflictFileName);
            LOG.warn("File already exists on server. Renaming " + absolutePath + " -> " + renamed);
            diskWriterService.replaceFile(absolutePath, renamed);
            return false;
        }

        return false;
    }

    private Path getAbsolutePath(String relativePath) {
        String relativePathWithoutLeadingSlash = relativePath;
        return syncDirectory.resolve(relativePathWithoutLeadingSlash);
    }
}
