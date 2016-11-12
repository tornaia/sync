package com.github.tornaia.sync.server.service;

import com.github.tornaia.sync.server.data.converter.FileToFileMetaInfoConverter;
import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.server.service.exception.*;
import com.github.tornaia.sync.server.websocket.SyncWebSocketHandler;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import static com.github.tornaia.sync.shared.api.RemoteEventType.*;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DIRECTORY_POSTFIX;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;

@Service
public class FileCommandService {

    private static final Logger LOG = LoggerFactory.getLogger(FileCommandService.class);

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private SyncWebSocketHandler syncWebSocketHandler;

    @Autowired
    private FileToFileMetaInfoConverter fileToFileMetaInfoConverter;

    @Autowired
    private S3Service s3Service;

    public FileMetaInfo createFile(String clientid, String userid, long size, long creationDateTime, long modificationDateTime, String path, InputStream content) throws IOException {
        if (content == null && size != 0L) {
            throw new IllegalStateException("When content is NULL then the file is a directory so the size must be zero. Content: " + content + ", size: " + size);
        }

        boolean isDirectory = path.endsWith(DIRECTORY_POSTFIX);
        String pathAsFile = isDirectory ? path.substring(0, path.length() - DIRECTORY_POSTFIX.length()) : path;
        String pathAsDirectory = isDirectory ? path : path + DIRECTORY_POSTFIX;

        File fileAsFile = fileRepository.findByUseridAndPath(userid, pathAsFile);
        File fileAsDirectory = fileRepository.findByUseridAndPath(userid, pathAsDirectory);
        if (fileAsFile != null || fileAsDirectory != null) {
            LOG.warn("CREATE File already exist: " + path);
            throw new FileAlreadyExistsException(path);
        }

        File file = fileRepository.insert(new File(userid, path, size, creationDateTime, modificationDateTime));
        FileMetaInfo fileMetaInfo = fileToFileMetaInfoConverter.convert(file);

        try {
            s3Service.putFile(fileMetaInfo, content == null ? new ByteArrayInputStream(new byte[0]) : content);
        } catch (DynamicStorageException e) {
            LOG.warn("Cleanup because saving data to S3 failed", e);
            fileRepository.delete(file);
            throw e;
        }

        syncWebSocketHandler.notifyClientsExceptForSource(clientid, new RemoteFileEvent(CREATED, fileMetaInfo));
        LOG.info("CREATE: " + fileMetaInfo);
        return fileMetaInfo;
    }

    public FileMetaInfo modifyFile(String clientid, String userid, String id, long oldSize, long oldCreationDateTime, long oldModificationDateTime, long newSize, long newCreationDateTime, long newModificationDateTime, InputStream newContent) throws IOException {
        if (newContent == null && newSize != 0L) {
            throw new IllegalStateException("When content is NULL then the file is a directory so the newSize must be zero. Content: " + newContent + ", newSize: " + newSize);
        }

        File file = fileRepository.findByUseridAndId(userid, id);
        if (file == null) {
            LOG.warn("MODIFY Not found file! userid: " + userid + ", id: " + id);
            throw new FileNotFoundException(userid, id);
        }

        if (file.getSize() != oldSize) {
            String message = "MODIFY Request based on outdated newSize: " + file + ", old: " + file.getSize() + ", new: " + oldSize;
            LOG.warn(message);
            throw new OutdatedException(file.getUserid(), file.getId());
        }

        if (file.getCreationDate() != oldCreationDateTime) {
            String message = "MODIFY Request based on outdated newCreationDateTime: " + file + ", old: " + file.getCreationDate() + ", new: " + oldCreationDateTime;
            LOG.warn(message);
            throw new OutdatedException(file.getUserid(), file.getId());
        }

        if (file.getLastModifiedDate() != oldModificationDateTime) {
            String message = "MODIFY Request based on outdated lastModifiedDateTime: " + file + ", old: " + file.getLastModifiedDate() + ", new: " + oldModificationDateTime;
            LOG.warn(message);
            throw new OutdatedException(file.getUserid(), file.getId());
        }

        file.setCreationDate(newCreationDateTime);
        file.setLastModifiedDate(newModificationDateTime);
        file.setSize(newSize);

        FileMetaInfo fileMetaInfo = fileToFileMetaInfoConverter.convert(file);
        s3Service.putFile(fileMetaInfo, newContent == null ? new ByteArrayInputStream(new byte[0]) : newContent);
        // FIXME what if the previous statement (update object in S3) works but the next (updating fileMetaInfo in Mongo) fails?
        fileRepository.save(file);

        syncWebSocketHandler.notifyClientsExceptForSource(clientid, new RemoteFileEvent(MODIFIED, fileMetaInfo));
        LOG.info("MODIFY: " + fileMetaInfo);
        return fileMetaInfo;
    }

    public void deleteFile(String clientid, String userid, String id, long size, long creationDateTime, long modificationDateTime) {
        File file = fileRepository.findByUseridAndId(userid, id);
        if (file == null) {
            LOG.warn("DELETE File not found: " + id);
            throw new FileNotFoundException(userid, id);
        }

        if (file.getSize() != size) {
            LOG.warn("DELETE File attributes mismatch: " + file + ", vs: " + size + ", " + creationDateTime + ", " + modificationDateTime);
            throw new OutdatedException(userid, id);
        }
        if (file.isFile() && file.getCreationDate() != creationDateTime && file.getLastModifiedDate() != modificationDateTime) {
            LOG.warn("DELETE File attributes mismatch: " + file + ", vs: " + size + ", " + creationDateTime + ", " + modificationDateTime);
            throw new OutdatedException(userid, id);
        }

        if (file.isDirectory()) {
            String directoryPathWithPostfix = file.getRelativePath();
            String directoryPath = directoryPathWithPostfix.substring(0, directoryPathWithPostfix.length() - DOT_FILENAME.length());
            String directoryPathEscapedForRegex = Pattern.quote(directoryPath);
            String directoryPathFullyEscaped = StringEscapeUtils.escapeJava(directoryPathEscapedForRegex);
            List<File> filesUnderThisDirectory = fileRepository.findByUseridAndPathStartsWith(userid, directoryPathFullyEscaped);
            if (filesUnderThisDirectory.size() > 1) {
                LOG.warn("DELETE Directory is not empty: " + directoryPath + ", files under it: " + filesUnderThisDirectory);
                throw new DirectoryNotEmptyException(directoryPath);
            }
        }

        FileMetaInfo deletedFileMetaInfo = fileToFileMetaInfoConverter.convert(file);
        fileRepository.delete(file);

        deleteFileFromDynamicStorageQuietly(id);

        syncWebSocketHandler.notifyClientsExceptForSource(clientid, new RemoteFileEvent(DELETED, deletedFileMetaInfo));
        LOG.info("DELETE: " + deletedFileMetaInfo.relativePath);
    }

    public void deleteAll() {
        List<File> files = fileRepository.findAll();
        LOG.info("Deleting all files from DB: " + files.size());
        files.forEach(fileRepository::delete);
        files.stream().map(fileToFileMetaInfoConverter::convert).map(fmi -> fmi.id).forEach(s3Service::deleteFile);
        LOG.info("All files deleted from DB");
    }

    private void deleteFileFromDynamicStorageQuietly(String id) {
        LOG.debug("Delete file from dynamic storage quietly: " + id);
        try {
            s3Service.deleteFile(id);
        } catch (DynamicStorageException e) {
            LOG.warn("Failed to delete file from dynamic storage" + id, e);
        }
    }
}
