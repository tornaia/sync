package com.github.tornaia.sync.client.win.statestorage;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SyncStateSnapshot implements Serializable {

    public long lastServerInfoAt = Long.MIN_VALUE;

    private Map<String, FileMetaInfo> syncState = new ConcurrentHashMap<>();

    public FileMetaInfo get(String relativePath) {
        return syncState.get(relativePath);
    }

    public void put(FileMetaInfo fileMetaInfo) {
        syncState.put(fileMetaInfo.relativePath, fileMetaInfo);
    }

    public void remove(String relativePath) {
        syncState.remove(relativePath);
    }

    public void replace(FileMetaInfo oldFileMetaInfo, FileMetaInfo newFileMetaInfo) {
        if (!Objects.equals(oldFileMetaInfo.relativePath, newFileMetaInfo.relativePath)) {
            throw new IllegalArgumentException("RelativePath must be equal: " + oldFileMetaInfo + ", " + newFileMetaInfo);
        }
        syncState.replace(oldFileMetaInfo.relativePath, oldFileMetaInfo, newFileMetaInfo);
    }
}
