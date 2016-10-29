package com.github.tornaia.sync.client.win.remote.writer;

import com.github.tornaia.sync.client.win.local.writer.DiskWriterService;
import com.github.tornaia.sync.client.win.remote.RemoteKnownState;
import com.github.tornaia.sync.shared.api.CreateFileResponse;
import com.github.tornaia.sync.shared.api.DeleteFileResponse;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.ModifyFileResponse;
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
import java.util.List;
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

        CreateFileResponse createFileResponse = remoteRestCommandService.onFileCreate(localFileMetaInfo, file);
        boolean ok = Objects.equals(CreateFileResponse.Status.OK, createFileResponse.status);
        if (ok) {
            LOG.info("File created on server: " + createFileResponse.fileMetaInfo);
            remoteKnownState.add(createFileResponse.fileMetaInfo);
            return true;
        }

        boolean alreadyExist = Objects.equals(CreateFileResponse.Status.ALREADY_EXIST, createFileResponse.status);
        if (alreadyExist) {
            if (localFileMetaInfo.isFile()) {
                diskWriterService.handleConflictOfFile(absolutePath, localFileMetaInfo);
                return true;
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

        boolean transferFailed = Objects.equals(CreateFileResponse.Status.TRANSFER_FAILED, createFileResponse.status);
        if (transferFailed) {
            LOG.warn("Transfer failed: " + createFileResponse.fileMetaInfo + ", " + createFileResponse.message);
            return false;
        }

        throw new IllegalStateException("Unhandled state");
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
        FileMetaInfo oldRemoteFileMetaInfo = optionalRemoteFileMetaInfo.get();
        if (Objects.equals(oldRemoteFileMetaInfo, localFileMetaInfo)) {
            LOG.debug("File is already known by server: " + localFileMetaInfo);
            return true;
        }

        FileMetaInfo newLocalFileMetaInfo;
        try {
            newLocalFileMetaInfo = new FileMetaInfo(oldRemoteFileMetaInfo.id, localFileMetaInfo.userid, localFileMetaInfo.relativePath, file);
        } catch (IOException e) {
            LOG.warn("Cannot read file from disk", e);
            return false;
        }

        ModifyFileResponse modifyFileResponse = remoteRestCommandService.onFileModify(oldRemoteFileMetaInfo, newLocalFileMetaInfo, file);
        boolean ok = Objects.equals(ModifyFileResponse.Status.OK, modifyFileResponse.status);
        if (ok) {
            LOG.info("File modified on server: " + modifyFileResponse.fileMetaInfo);
            remoteKnownState.add(modifyFileResponse.fileMetaInfo);
            return true;
        }

        boolean notFound = Objects.equals(ModifyFileResponse.Status.NOT_FOUND, modifyFileResponse.status);
        if (notFound) {
            LOG.info("Updating file's content on server failed since its not there. Create now file on server: " + relativePath);
            return createFile(relativePath);
        }

        boolean outdated = Objects.equals(ModifyFileResponse.Status.OUTDATED, modifyFileResponse.status);
        if (outdated) {
            diskWriterService.handleConflictOfFile(absolutePath, localFileMetaInfo);
            return true;
        }

        boolean transferFailed = Objects.equals(ModifyFileResponse.Status.TRANSFER_FAILED, modifyFileResponse.status);
        if (transferFailed) {
            LOG.warn("Transfer failed: " + modifyFileResponse.fileMetaInfo + ", " + modifyFileResponse.message);
            return false;
        }

        throw new IllegalStateException("Unhandled state");
    }

    public boolean deleteFile(String relativePath) {
        Optional<FileMetaInfo> optionalRemoteFileMetaInfo = remoteKnownState.get(relativePath);
        if (!optionalRemoteFileMetaInfo.isPresent()) {
            LOG.info("File is already deleted from server: " + relativePath);
            return true;
        }

        FileMetaInfo fileMetaInfo = optionalRemoteFileMetaInfo.get();

        File localFile = getAbsolutePath(relativePath).toFile();
        boolean localFileExistWithThisRelativePath = localFile.exists();
        if (localFileExistWithThisRelativePath) {
            LOG.info("Do not delete a file on server that exists on disk: " + fileMetaInfo);
            return true;
        }

        if (fileMetaInfo.isDirectory()) {
            List<FileMetaInfo> allFilesUnderRelativePath = remoteKnownState.getAllChildrenOrderedByPathLength(fileMetaInfo.relativePath);
            allFilesUnderRelativePath
                    .stream()
                    .map(fmi -> fmi.relativePath)
                    .filter(rp -> !rp.equals(fileMetaInfo.relativePath))
                    .forEachOrdered(this::deleteFile);
        }

        DeleteFileResponse deleteFileResponse = remoteRestCommandService.onFileDelete(fileMetaInfo);

        boolean ok = Objects.equals(DeleteFileResponse.Status.OK, deleteFileResponse.status);
        if (ok) {
            LOG.info("File deleted from server: " + fileMetaInfo);
            remoteKnownState.remove(fileMetaInfo);
            return true;
        }

        boolean notFound = Objects.equals(DeleteFileResponse.Status.NOT_FOUND, deleteFileResponse.status);
        if (notFound) {
            LOG.info("File was not deleted from server. It knows nothing about this file: " + fileMetaInfo);
            remoteKnownState.remove(fileMetaInfo);
            return true;
        }

        boolean outdated = Objects.equals(DeleteFileResponse.Status.OUTDATED, deleteFileResponse.status);
        if (outdated) {
            LOG.warn("File was not deleted from server. It might have an another state: " + fileMetaInfo);
            remoteKnownState.remove(fileMetaInfo);
            return true;
        }

        boolean transferFailed = Objects.equals(DeleteFileResponse.Status.TRANSFER_FAILED, deleteFileResponse.status);
        if (transferFailed) {
            LOG.warn("Transfer failed: " + fileMetaInfo + ", " + deleteFileResponse.message);
            return false;
        }

        throw new IllegalStateException("Unhandled state");
    }

    private Path getAbsolutePath(String relativePath) {
        return syncDirectory.resolve(relativePath);
    }
}
