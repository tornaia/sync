package com.github.tornaia.sync.client.win.statestorage;

import com.github.tornaia.sync.client.win.httpclient.RecentChangesResponse;
import com.github.tornaia.sync.client.win.httpclient.RestHttpClient;
import com.github.tornaia.sync.client.win.watchservice.DiskWatchService;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;

@Component
public class SyncStateManager {

    private static final Logger LOG = LoggerFactory.getLogger(SyncStateManager.class);

    @Value("${client.state.file.path:C:\\temp2\\client.db}")
    private String stateFilePath;

    @Autowired
    private RestHttpClient restHttpClient;

    @Autowired
    private DiskWatchService diskWatchService;

    private SyncStateSnapshot syncStateSnapshot;

    @PostConstruct
    public void init() {
        if (!new File(stateFilePath).exists()) {
            syncStateSnapshot = new SyncStateSnapshot();
        } else {
            try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(stateFilePath))) {
                syncStateSnapshot = (SyncStateSnapshot) objectInputStream.readObject();
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException("Cannot read sync syncStateSnapshot file", e);
            }
        }

        writeSyncClientStateToDisk();

        fetchAllDataSinceLastUpdate();
    }

    // TODO remove this, use webSocket and send on connection init the lastServerInfoAt timestamp and the the server will push back the filemetainfo message
    public void fetchAllDataSinceLastUpdate() {
        long when = System.currentTimeMillis();
        RecentChangesResponse recentChangesResponse = restHttpClient.getAllAfter(syncStateSnapshot.lastServerInfoAt);
        if (recentChangesResponse.status == RecentChangesResponse.Status.TRANSFER_FAILED) {
            LOG.warn("Client is offline! Cannot get updates from server!");
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
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(stateFilePath))) {
            objectOutputStream.writeObject(syncStateSnapshot);
            objectOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException("Cannot write syncStateSnapshot to disk", e);
        }
    }
}
