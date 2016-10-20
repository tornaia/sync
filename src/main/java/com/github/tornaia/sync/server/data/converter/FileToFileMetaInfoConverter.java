package com.github.tornaia.sync.server.data.converter;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.springframework.stereotype.Component;

@Component
public class FileToFileMetaInfoConverter {

    public FileMetaInfo convert(File file) {
        return new FileMetaInfo(file.getId(), file.getUserid(), file.getPath(), file.getSize(), file.getCreationDate(), file.getLastModifiedDate());
    }
}
