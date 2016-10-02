package com.github.tornaia.sync.client.win.statestorage;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncStateSnapshot implements Serializable {

    public static long lastServerInfoAt = -1;

    public Map<String, FileMetaInfo> syncState = new ConcurrentHashMap<>();

    public void put(FileMetaInfo fileMetaInfo) {
        syncState.put(fileMetaInfo.relativePath, fileMetaInfo);
    }

    public void remove(String relativePath) {
        syncState.remove(relativePath);
    }
}
