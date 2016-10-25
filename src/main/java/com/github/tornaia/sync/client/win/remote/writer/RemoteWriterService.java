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
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DIRECTORY_POSTFIX;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;

@Component
public class RemoteWriterService {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteWriterService.class);

    @Value("${client.sync.userid}")
    private String userid;

    @Value("${client.sync.directory.path}")
    private String directoryPath;

    @Autowired
    private RemoteRestCommandService remoteRestCommandService;

    @Autowired
    private DiskWriterService diskWriterService;

    @Autowired
    private RemoteKnownState remoteKnownState;

    private Path syncDirectory;

    @PostConstruct
    public void init() {
        syncDirectory = new File(directoryPath).toPath();
    }

    public boolean createFile(String relativePath) {
        Path absolutePath = getAbsolutePath(relativePath);
        File file = absolutePath.toFile();
        if (absolutePath.toFile().isDirectory() && !absolutePath.toFile().getAbsolutePath().endsWith(DIRECTORY_POSTFIX)) {
            absolutePath = absolutePath.resolve(DOT_FILENAME);
            file = absolutePath.toFile();
            relativePath = relativePath + DIRECTORY_POSTFIX;
        }

        FileMetaInfo localFileMetaInfo;
        try {
            localFileMetaInfo = FileMetaInfo.createNonSyncedFileMetaInfo(userid, relativePath, file);
        } catch (NoSuchFileException e) {
            LOG.warn("Skipping created event of a non-existing file: " + relativePath);
            return true;
        } catch (IOException e) {
            LOG.error("Cannot read file", e);
            return false;
        }

        Optional<FileMetaInfo> optionalRemoteFileMetaInfo = remoteKnownState.get(relativePath);
        if (optionalRemoteFileMetaInfo.isPresent() && Objects.equals(optionalRemoteFileMetaInfo.get(), localFileMetaInfo)) {
            LOG.debug("File is already known by server: " + localFileMetaInfo);
            return true;
        }

        FileCreateResponse fileCreateResponse = remoteRestCommandService.onFileCreate(localFileMetaInfo, file);
        boolean ok = Objects.equals(FileCreateResponse.Status.OK, fileCreateResponse.status);
        if (ok) {
            LOG.info("File created on server: " + fileCreateResponse.fileMetaInfo);
            remoteKnownState.add(fileCreateResponse.fileMetaInfo);
            return true;
        }

        boolean conflict = Objects.equals(FileCreateResponse.Status.CONFLICT, fileCreateResponse.status);
        if (conflict) {
            if (localFileMetaInfo.isFile()) {
                diskWriterService.handleConflictOfFile(absolutePath, localFileMetaInfo);
            } else {
                if (optionalRemoteFileMetaInfo.isPresent()) {
                    LOG.info("Directory already is on disk. Attributes are different but wont update: " + optionalRemoteFileMetaInfo.get());
                    return true;
                } else {
                    LOG.info("RemoteFileMetaInfo does not exist but then how do we have a notFound for localFileMetaInfo: " + localFileMetaInfo);
                    return true;
                }
            }
        }

        return false;
    }

    public boolean modifyFile(String relativePath) {
        Path absolutePath = getAbsolutePath(relativePath);
        File file = absolutePath.toFile();

        FileMetaInfo localFileMetaInfo;
        try {
            localFileMetaInfo = FileMetaInfo.createNonSyncedFileMetaInfo(userid, relativePath, file);
        } catch (NoSuchFileException e) {
            LOG.info("Skipping modified event of a non-existing file: " + relativePath);
            return true;
        } catch (AccessDeniedException e) {
            LOG.warn("Cannot read file since it is use: " + relativePath);
            return false;
        } catch (IOException e) {
            LOG.error("Cannot read file", e);
            return false;
        }

        Optional<FileMetaInfo> optionalRemoteFileMetaInfo = remoteKnownState.get(relativePath);
        if (!optionalRemoteFileMetaInfo.isPresent()) {
            LOG.debug("Cannot modify a file that is unknown to server. So lets create instead of modification: " + localFileMetaInfo);
            return createFile(relativePath);
        }
        if (optionalRemoteFileMetaInfo.isPresent() && Objects.equals(optionalRemoteFileMetaInfo.get(), localFileMetaInfo)) {
            LOG.debug("File is already known by server: " + localFileMetaInfo);
            return true;
        }

        FileMetaInfo requestFileMetaInfo;
        try {
            requestFileMetaInfo = new FileMetaInfo(optionalRemoteFileMetaInfo.get().id, localFileMetaInfo.userid, localFileMetaInfo.relativePath, file);
        } catch (IOException e) {
            LOG.warn("Cannot read file from disk", e);
            return false;
        }

        FileModifyResponse fileModifyResponse = remoteRestCommandService.onFileModify(requestFileMetaInfo, file);
        boolean ok = Objects.equals(FileModifyResponse.Status.OK, fileModifyResponse.status);
        if (ok) {
            LOG.info("File modified on server: " + fileModifyResponse.fileMetaInfo);
            remoteKnownState.add(fileModifyResponse.fileMetaInfo);
            return true;
        }

        boolean conflict = Objects.equals(FileModifyResponse.Status.CONFLICT, fileModifyResponse.status);
        if (conflict) {
            diskWriterService.handleConflictOfFile(absolutePath, localFileMetaInfo);
            return false;
        }

        boolean notFound = Objects.equals(FileModifyResponse.Status.NOT_FOUND, fileModifyResponse.status);
        if (notFound) {
            LOG.info("Updating file's content on server failed since it was removed. Create now file on server: " + relativePath);
            return createFile(relativePath);
        }

        return false;
    }

    public boolean deleteFile(String relativePath) {
        Optional<FileMetaInfo> optionalRemoteFileMetaInfo = remoteKnownState.get(relativePath);
        if (!optionalRemoteFileMetaInfo.isPresent()) {
            LOG.info("File is already deleted from server: " + relativePath);
            return true;
        }

        FileMetaInfo fileMetaInfo = optionalRemoteFileMetaInfo.get();

        boolean localFileExistWithThisRelativePath = getAbsolutePath(relativePath).toFile().exists();
        if (localFileExistWithThisRelativePath) {
            LOG.info("Do not delete a file on server that exists on disk: " + fileMetaInfo);
            return true;
        }
        // FIXME here app should delete folders subfolders and subfiles recursively start with the leaves.

        FileDeleteResponse fileDeleteResponse = remoteRestCommandService.onFileDelete(fileMetaInfo);

        boolean ok = Objects.equals(FileDeleteResponse.Status.OK, fileDeleteResponse.status);
        if (ok) {
            LOG.info("File deleted from server: " + fileMetaInfo);
            remoteKnownState.remove(fileMetaInfo);
            return true;
        }

        boolean notFound = Objects.equals(FileDeleteResponse.Status.NOT_FOUND, fileDeleteResponse.status);
        if (notFound) {
            LOG.info("File was not deleted from server. It knows nothing about this file: " + fileDeleteResponse.fileMetaInfo);
            remoteKnownState.remove(fileMetaInfo);
            return true;
        }

        return false;
    }

    private Path getAbsolutePath(String relativePath) {
        return syncDirectory.resolve(relativePath);
    }
}
