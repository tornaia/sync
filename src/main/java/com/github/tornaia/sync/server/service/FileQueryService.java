package com.github.tornaia.sync.server.service;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.server.utils.FileUtil;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

import static com.github.tornaia.sync.server.utils.FileUtil.getFileMetaInfo;
import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

@Component
public class FileQueryService {

    private static final Logger LOG = LoggerFactory.getLogger(FileQueryService.class);

    @Autowired
    private FileRepository fileRepository;

    public List<FileMetaInfo> getModifiedFiles(String userid, long modTs) {
        List<File> fileList = fileRepository.findByUserIdAndLastModifiedDateAfter(userid, modTs);
        if (isNull(fileList)) {
            return emptyList();
        }

        return fileList.stream().map(FileUtil::getFileMetaInfo).collect(toList());
    }

    public File getFileById(String id) {
        File file = fileRepository.findOne(id);
        if (file == null) {
            throw new FileNotFoundException(id);
        }

        return file;
    }

    public FileMetaInfo getFileMetaInfoById(String id) {
        File file = getFileById(id);
        return getFileMetaInfo(file);
    }

    public FileMetaInfo getFileMetaInfoByPath(String path) {
        File file = fileRepository.findByPath(path);
        if (file == null) {
            throw new FileNotFoundException(path);
        }

        return getFileMetaInfo(file);
    }

    public List<FileMetaInfo> getAllFileMetaInfo(String userid) {
        List<File> fileList = fileRepository.findByUserid(userid);
        if (isNull(fileList)) {
            return emptyList();
        }

        return fileList.stream().map(FileUtil::getFileMetaInfo).collect(toList());
    }
}
