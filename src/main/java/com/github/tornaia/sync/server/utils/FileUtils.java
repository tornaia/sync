package com.github.tornaia.sync.server.utils;

import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.shared.api.FileMetaInfo;

// TODO I just simply do not like this class.. Maybe the name maybe Im just have a different taste
public final class FileUtils {

    private FileUtils() {
    }

    public static FileMetaInfo getFileMetaInfo(File file) {
        return new FileMetaInfo(file.getId(), file.getUserid(), file.getPath(), file.getData().length, file.getCreationDate(), file.getLastModifiedDate());
    }
}
