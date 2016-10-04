package com.github.tornaia.sync.client.win.statestorage;

import com.github.tornaia.sync.client.win.httpclient.RestHttpClient;
import com.github.tornaia.sync.client.win.httpclient.RecentChangesResponse;
import com.github.tornaia.sync.client.win.watchservice.DiskWatchService;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;

@Component
public class SyncStateManager {

    private static final String STATE_FILE_PATH = "C:\\temp2\\sync-client-win.db";

    @Autowired
    private RestHttpClient restHttpClient;

    @Autowired
    private DiskWatchService diskWatchService;

    private SyncStateSnapshot syncStateSnapshot;

    public SyncStateManager() {
        if (!new File(STATE_FILE_PATH).exists()) {
            syncStateSnapshot = new SyncStateSnapshot();
        } else {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(STATE_FILE_PATH))) {
                syncStateSnapshot = (SyncStateSnapshot) objectInputStream.readObject();
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException("Cannot read sync syncStateSnapshot file", e);
            }
        }

        writeSyncClientStateToDisk();
    }

    // TODO remove this, use webSocket and send on connection init the lastServerInfoAt timestamp and the the server will push back the filemetainfo message
    @PostConstruct
    public void fetchAllDataSinceLastUpdate() {
        long when = System.currentTimeMillis();
        RecentChangesResponse recentChangesResponse = restHttpClient.getAllAfter(syncStateSnapshot.lastServerInfoAt);
        if (recentChangesResponse.status == RecentChangesResponse.Status.TRANSFER_FAILED) {
            System.out.println("Client is offline! Cannot get updates from server!");
            return;
        }

        for (FileMetaInfo fileMetaInfo : recentChangesResponse.fileMetaInfos) {
            byte[] content = restHttpClient.getFile(fileMetaInfo);
            diskWatchService.writeToDisk(fileMetaInfo, content);
            syncStateSnapshot.put(fileMetaInfo);
        }

        syncStateSnapshot.lastServerInfoAt = when;
        writeSyncClientStateToDisk();
    }

    public FileMetaInfo getFileMetaInfo(String relativePath) {
        return syncStateSnapshot.syncState.get(relativePath);
    }

    public void onFileModify(FileMetaInfo fileMetaInfo) {
        syncStateSnapshot.put(fileMetaInfo);
        writeSyncClientStateToDisk();
    }

    public void onFileDelete(String relativePath) {
        syncStateSnapshot.remove(relativePath);
        writeSyncClientStateToDisk();
    }

    private void writeSyncClientStateToDisk() {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(STATE_FILE_PATH))) {
            objectOutputStream.writeObject(syncStateSnapshot);
            objectOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot write syncStateSnapshot to disk", e);
        }
    }
}
