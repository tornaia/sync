package com.github.tornaia.sync.client.win.local.writer;

import com.github.tornaia.sync.client.win.util.FileUtils;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

@Component
public class DiskWriterService {

    private static final Logger LOG = LoggerFactory.getLogger(DiskWriterService.class);

    @Value("${frosch-sync.userid:7247234}")
    private String userid;

    @Value("${client.sync.directory.path:C:\\temp\\client}")
    private String syncDirectoryPath;

    @Autowired
    private FileUtils fileUtils;

    private Path syncDirectory;

    @PostConstruct
    public void init() {
        syncDirectory = new File(syncDirectoryPath).toPath();
    }

    public Optional<Path> createTempFile(byte[] fileContent, long creationDateTime, long modificationDateTime) {
        Path tempFile;
        try {
            tempFile = fileUtils.createWorkFile();
        } catch (IOException e) {
            LOG.error("Cannot create temporary file", e);
            return Optional.empty();
        }

        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            IOUtils.write(fileContent, fos);
        } catch (IOException e) {
            LOG.error("Cannot write temporary file", e);
            return Optional.empty();
        }

        try {
            fileUtils.setCreationTime(tempFile, creationDateTime);
            fileUtils.setLastModifiedTime(tempFile, modificationDateTime);
        } catch (IOException e) {
            LOG.error("Cannot set temporary file's attributes", e);
            return Optional.empty();
        }

        return Optional.of(tempFile);
    }

    public boolean writeFileAtomically(Path absolutePath, byte[] fileContent, long creationDateTime, long modificationDateTime) {
        Optional<Path> tempFile = createTempFile(fileContent, creationDateTime, modificationDateTime);

        if (!tempFile.isPresent()) {
            LOG.error("Cannot create temporary file");
            return false;
        }

        if (!absolutePath.toFile().getParentFile().mkdirs()) {
            if (!absolutePath.toFile().getParentFile().isDirectory()) {
                LOG.error("Cannot create directories to target");
                return false;
            }
            LOG.trace("Directory to target already exist");
        }

        File file = absolutePath.toFile();
        if (file.exists()) {
            String relativePath = getRelativePath(file);
            FileMetaInfo localFileMetaInfo;
            try {
                localFileMetaInfo = FileMetaInfo.createNonSyncedFileMetaInfo(userid, relativePath, file);
            } catch (IOException e) {
                LOG.error("Cannot read file", e);
                return false;
            }

            FileMetaInfo newFileMetaInfo = new FileMetaInfo(null, userid, relativePath, file.length(), creationDateTime, modificationDateTime);
            if (Objects.equals(newFileMetaInfo, localFileMetaInfo)) {
                LOG.info("File already is on disk: " + newFileMetaInfo);
                return true;
            } else {
                handleConflict(absolutePath, localFileMetaInfo);
            }
        }

        try {
            // TODO StandardCopyOption.COPY_ATTRIBUTES
            Files.move(tempFile.get(), absolutePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("Cannot move temporary file to target", e);
            return false;
        }

        try {
            fileUtils.setCreationTime(absolutePath, creationDateTime);
            fileUtils.setLastModifiedTime(absolutePath, modificationDateTime);
        } catch (IOException e) {
            LOG.error("Cannot set file's attributes", e);
            return false;
        }

        LOG.trace("File was written to disk: " + absolutePath.toFile().getAbsolutePath() + ", creationDateTime: " + creationDateTime + ", modificationDateTime: " + modificationDateTime + ", bytes: " + fileContent.length);
        return true;
    }

    private void handleConflict(Path absolutePath, FileMetaInfo localFileMetaInfo) {
        // TODO move this conflict file name creation to a separate object
        String originalFileName = absolutePath.toFile().getAbsolutePath();
        boolean hasExtension = originalFileName.indexOf('.') != -1;
        String postFix = "_conflict_" + localFileMetaInfo.length + "_" + localFileMetaInfo.creationDateTime + "_" + localFileMetaInfo.modificationDateTime;
        String conflictFileName = hasExtension ? originalFileName.split("\\.", 2)[0] + postFix + "." + originalFileName.split("\\.", 2)[1] : originalFileName + postFix;
        // TODO what should happen when this renamed/conflictFileName file exists?
        Path renamed = new File(absolutePath.toFile().getParentFile().getAbsolutePath()).toPath().resolve(conflictFileName);
        LOG.warn("File already exists on server. Renaming " + absolutePath + " -> " + renamed);
        replaceFileAtomically(absolutePath, renamed);
    }

    // TODO rename replace Without Delete
    public boolean replaceFileAtomically(Path source, Path target) {
        LOG.trace("Replace " + target.toFile().getAbsolutePath() + " with " + source.toFile().getAbsolutePath());
        try {
            // TODO StandardCopyOption.COPY_ATTRIBUTES
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOG.error("Cannot write target file", e);
            return false;
        }
        return true;
    }

    public boolean delete(Path fileAbsolutePath) {
        LOG.trace("Delete " + fileAbsolutePath.toFile().getAbsolutePath());
        try {
            Files.delete(fileAbsolutePath);
        } catch (IOException e) {
            LOG.error("Cannot write target file", e);
            return false;
        }
        return true;
    }

    private String getRelativePath(File file) {
        return syncDirectory.relativize(file.toPath()).toString();
    }
}
