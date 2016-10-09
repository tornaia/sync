package com.github.tornaia.sync.server.service;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.server.service.exception.FileAlreadyExistsException;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.server.utils.FileUtils;
import com.github.tornaia.sync.server.websocket.SyncWebSocketHandler;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import com.github.tornaia.sync.shared.api.RemoteFileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
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

    public void createFile(String userid, long creationDateTime, long modificationDateTime, String path, byte[] content) throws IOException {
        File file = fileRepository.findByUseridAndPath(userid, path);
        if (!Objects.isNull(file)) {
            throw new FileAlreadyExistsException(path);
        }

        file = fileRepository.insert(new File(userid, path, content, creationDateTime, modificationDateTime));
        FileMetaInfo fileMetaInfo = FileUtils.getFileMetaInfo(file);
        syncWebSocketHandler.notifyClients(new RemoteFileEvent(CREATED, fileMetaInfo));
        LOG.info("CREATE file: " + fileMetaInfo);
    }

    public void updateFile(String id, long creationDateTime, long modificationDateTime, byte[] content) throws IOException {
        File file = fileRepository.findOne(id);
        if (Objects.isNull(file)) {
            throw new FileNotFoundException(id);
        } else {
            file.setCreationDate(creationDateTime);
            file.setLastModifiedDate(modificationDateTime);
            file.setData(content);
            fileRepository.save(file);
            syncWebSocketHandler.notifyClients(new RemoteFileEvent(MODIFIED, FileUtils.getFileMetaInfo(file)));
        }
    }

    public void deleteFile(String id) {
        File file = fileRepository.findOne(id);
        if (Objects.isNull(file)) {
            throw new FileNotFoundException(id);
        }
        String path = file.getPath();
        fileRepository.delete(file);
        syncWebSocketHandler.notifyClients(new RemoteFileEvent(DELETED, FileUtils.getFileMetaInfo(file)));
        LOG.info("DELETE file: " + path);
    }

    public void deleteAll() {
        List<File> files = fileRepository.findAll();
        files.forEach(fileRepository::delete);
        LOG.info("Just all files from DB: " + files.size());
    }
}
