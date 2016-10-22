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
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;

@Component
public class DiskWriterService {

    private static final Logger LOG = LoggerFactory.getLogger(DiskWriterService.class);

    @Value("${frosch-sync.userid}")
    private String userid;

    @Value("${client.sync.directory.path}")
    private String syncDirectoryPath;

    @Autowired
    private ConflictFileNameGenerator conflictFileNameGenerator;

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

    public Optional<Path> createTempDirectory(long creationDateTime, long modificationDateTime) {
        Path tempDirectory;
        try {
            tempDirectory = fileUtils.createWorkDirectory();
        } catch (IOException e) {
            LOG.error("Cannot create temporary directory", e);
            return Optional.empty();
        }

        try {
            fileUtils.setCreationTime(tempDirectory, creationDateTime);
        } catch (IOException e) {
            LOG.error("Cannot set temporary directory's creationTime", e);
            return Optional.empty();
        }

        return Optional.of(tempDirectory);
    }

    public boolean writeFileAtomically(Path absolutePath, byte[] fileContent, long creationDateTime, long modificationDateTime) {
        if (absolutePath.endsWith(DOT_FILENAME)) {
            return writeDirectoryAtomically(absolutePath, creationDateTime, modificationDateTime);
        }

        Optional<Path> tempFile = createTempFile(fileContent, creationDateTime, modificationDateTime);

        if (!tempFile.isPresent()) {
            LOG.error("Cannot create temporary file");
            return false;
        }

        boolean parentDirectoryExist = absolutePath.toFile().getParentFile().exists();
        if (!parentDirectoryExist) {
            boolean parentDirectoryCreated = absolutePath.toFile().getParentFile().mkdirs();
            if (!parentDirectoryCreated) {
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
                handleConflictOfFile(absolutePath, localFileMetaInfo);
            }
        }

        try {
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

    private boolean writeDirectoryAtomically(Path absolutePath, long creationDateTime, long modificationDateTime) {
        if (!absolutePath.endsWith(DOT_FILENAME)) {
            throw new IllegalStateException("Directory's absolutePath must end with .");
        }

        Optional<Path> tempDirectory = createTempDirectory(creationDateTime, modificationDateTime);

        if (!tempDirectory.isPresent()) {
            LOG.error("Cannot create temporary directory");
            return false;
        }

        boolean parentDirectoryExist = absolutePath.toFile().getParentFile().getParentFile().exists();
        if (!parentDirectoryExist) {
            boolean parentDirectoryCreated = absolutePath.toFile().getParentFile().getParentFile().mkdirs();
            if (!parentDirectoryCreated) {
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
                LOG.info("Directory already is on disk. Attributes are ok: " + newFileMetaInfo);
                return true;
            } else {
                LOG.info("Directory already is on disk. Attributes are different but wont update: " + newFileMetaInfo);
                return true;
            }
        }

        try {
            Files.move(tempDirectory.get(), absolutePath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("Cannot move temporary file to target", e);
            return false;
        }

        try {
            fileUtils.setCreationTime(absolutePath, creationDateTime);
        } catch (IOException e) {
            LOG.error("Cannot set directory's creationTime", e);
            return false;
        }

        LOG.trace("Directory was written to disk: " + absolutePath.toFile().getAbsolutePath() + ", creationDateTime: " + creationDateTime + ", modificationDateTime: " + modificationDateTime);
        return true;
    }

    public void handleConflictOfFile(Path absolutePath, FileMetaInfo localFileMetaInfo) {
        Path renamed = conflictFileNameGenerator.resolve(absolutePath, localFileMetaInfo);
        replaceFileAtomically(absolutePath, renamed);
    }

    public boolean replaceFileAtomically(Path source, Path target) {
        LOG.trace("Replace " + target.toFile().getAbsolutePath() + " with " + source.toFile().getAbsolutePath());
        boolean isDirectory = target.endsWith(DOT_FILENAME);
        if (isDirectory) {
            try {
                long remoteCreationTime = fileUtils.getCreationTime(source);
                long localCreationTime = fileUtils.getCreationTime(target);
                if (remoteCreationTime != localCreationTime) {
                    fileUtils.setCreationTime(target, remoteCreationTime);
                }
            } catch (IOException e) {
                LOG.error("Cannot write target directory", e);
                return false;
            }
        } else {
            try {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                LOG.error("Cannot write target file", e);
                return false;
            }
        }

        return true;
    }

    public boolean delete(Path fileAbsolutePath) {
        LOG.trace("Delete " + fileAbsolutePath.toFile().getAbsolutePath());
        try {
            Files.delete(fileAbsolutePath);
        } catch (DirectoryNotEmptyException e) {
            LOG.warn("Cannot delete directory. It is not empty: " + fileAbsolutePath);
            return true;
        } catch (IOException e) {
            LOG.warn("Cannot delete target file", e);
            return false;
        }
        return true;
    }

    private String getRelativePath(File file) {
        return syncDirectory.relativize(file.toPath()).toString();
    }
}
