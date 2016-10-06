package com.github.tornaia.sync.server.utils;


import com.github.tornaia.sync.server.data.document.File;
import com.github.tornaia.sync.shared.api.FileMetaInfo;

public final class FileUtil {

    private FileUtil() {
    }

    public static FileMetaInfo getFileMetaInfo(File file) {
        return new FileMetaInfo(file.getId(), file.getUserid(), file.getPath(), file.getData().length, file.getCreationDate(), file.getLastModifiedDate());
    }
}
