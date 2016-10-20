package com.github.tornaia.sync.server.service;

import com.github.tornaia.sync.server.data.converter.FileToFileMetaInfoConverter;
import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.server.websocket.SyncWebSocketHandler;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import static com.github.tornaia.sync.shared.api.RemoteEventType.*;

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
        File file = fileRepository.findByUseridAndPath(userid, path);
        if (!Objects.isNull(file)) {
            throw new FileAlreadyExistsException(path);
        }

        file = fileRepository.insert(new File(userid, path, size, creationDateTime, modificationDateTime));
        FileMetaInfo fileMetaInfo = fileToFileMetaInfoConverter.convert(file);
        s3Service.putFile(fileMetaInfo, content);
        syncWebSocketHandler.notifyClientsExceptForSource(clientid, new RemoteFileEvent(CREATED, fileMetaInfo));
        LOG.info("CREATE file: " + fileMetaInfo);
        return fileMetaInfo;
    }


    public void modifyFile(String clientid, String id, long size, long creationDateTime, long modificationDateTime, InputStream content) throws IOException {
        File file = fileRepository.findOne(id);
        if (Objects.isNull(file)) {
            LOG.info("MODIFY Not found file: " + id);
            throw new FileNotFoundException(id);
        } else {
            file.setCreationDate(creationDateTime);
            file.setLastModifiedDate(modificationDateTime);
            file.setSize(size);
            fileRepository.save(file);
            FileMetaInfo fileMetaInfo = fileToFileMetaInfoConverter.convert(file);
            s3Service.putFile(fileMetaInfo, content);
            syncWebSocketHandler.notifyClientsExceptForSource(clientid, new RemoteFileEvent(MODIFIED, fileMetaInfo));
            LOG.info("MODIFY file: " + fileMetaInfo);
        }
    }

    public void deleteFile(String clientid, String id) {
        File file = fileRepository.findOne(id);
        if (Objects.isNull(file)) {
            LOG.info("DELETE not found file: " + id);
            throw new FileNotFoundException(id);
        }
        String path = file.getPath();
        fileRepository.delete(file);
        FileMetaInfo deletedFileMetaInfo = fileToFileMetaInfoConverter.convert(file);
        s3Service.deleteFile(deletedFileMetaInfo);
        syncWebSocketHandler.notifyClientsExceptForSource(clientid, new RemoteFileEvent(DELETED, deletedFileMetaInfo));
        LOG.info("DELETE file: " + path);
    }

    public void deleteAll() {
        List<File> files = fileRepository.findAll();
        files.forEach(fileRepository::delete);
        files.stream().map(fileToFileMetaInfoConverter::convert).forEach(s3Service::deleteFile);
        LOG.info("All files deleted from DB: " + files.size());
    }
}
