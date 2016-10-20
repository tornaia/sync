package com.github.tornaia.sync.server.service;

import com.github.tornaia.sync.server.data.converter.FileToFileMetaInfoConverter;
import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.server.data.repository.FileRepository;
import com.github.tornaia.sync.server.service.exception.FileNotFoundException;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Component
public class FileQueryService {

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileToFileMetaInfoConverter fileToFileMetaInfoConverter;

    @Autowired
    private S3Service s3Service;

    public List<FileMetaInfo> getModifiedFiles(String userid, long modTs) {
        List<File> fileList = fileRepository.findByUseridAndLastModifiedDateAfter(userid, modTs);
        if (fileList == null) {
            return emptyList();
        }

        return fileList.stream().map(fileToFileMetaInfoConverter::convert).collect(toList());
    }

    public FileMetaInfo getFileMetaInfoById(String id) {
        File file = getFileById(id);
        return fileToFileMetaInfoConverter.convert(file);
    }

    public File getFileById(String id) {
        File file = fileRepository.findOne(id);
        if (file == null) {
            throw new FileNotFoundException(id);
        }

        return file;
    }

    public InputStream getContent(String id) {
        return s3Service.get(id);
    }
}
