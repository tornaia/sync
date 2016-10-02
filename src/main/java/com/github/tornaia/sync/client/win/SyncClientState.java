package com.github.tornaia.sync.client.win;

import com.github.tornaia.sync.shared.api.FileMetaInfo;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncClientState implements Serializable {

    private static long lastServerInfoAt = -1;
    private static final Map<String, FileMetaInfo> SYNC_STATE = new ConcurrentHashMap<>();

    public void put(String relativePathWithinSyncFolder, File file) {
        try {
            FileMetaInfo fileMetaInfo = new FileMetaInfo(relativePathWithinSyncFolder, file);
            SYNC_STATE.put(relativePathWithinSyncFolder, fileMetaInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
