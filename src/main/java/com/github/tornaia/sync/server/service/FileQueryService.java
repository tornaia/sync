package com.github.tornaia.sync.server.service;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class FileQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(FileQueryService.class);

    @Autowired
    private FileRepository fileRepository;

    public List<FileMetaInfo> getModifiedFiles(String userid, long modTs) {
        List<FileMetaInfo> result = new ArrayList<>();
        List<File> fileList = fileRepository.findByUserIdAndLastModifiedDateAfter(userid, modTs);
        if (!Objects.isNull(fileList)) {
            result.addAll(fileList.stream().map(file -> new FileMetaInfo(file.getId(), userid, file.getPath(), file.getData().length, file.getCreationDate(), file.getLastModifiedDate())).collect(Collectors.toList()));
        }
        return result;
    }

    public File getFileById(String id) {
        File file = fileRepository.findOne(id);
        if (file == null) {
            throw new FileNotFoundException(id);
        }

        return file;
    }

    public FileMetaInfo getMetaInfoById(String id) throws IOException {
        File file = fileRepository.findOne(id);
        if (file == null) {
            throw new FileNotFoundException(id);
        }

        FileMetaInfo fileMetaInfo = new FileMetaInfo(file.getId(), file.getUserid(), file.getPath(), file.getData().length, file.getCreationDate(), file.getLastModifiedDate());
        LOG.info("GET metaInfo: " + fileMetaInfo);
        return fileMetaInfo;
    }

    public FileMetaInfo getFileMetaInfoByPath(String path) {
        File file = fileRepository.findByPath(path);
        if (file == null) {
            throw new FileNotFoundException(file.getPath());
        }

        return new FileMetaInfo(file.getId(), file.getUserid(), file.getPath(), file.getData().length, file.getCreationDate(), file.getLastModifiedDate());
    }
}
