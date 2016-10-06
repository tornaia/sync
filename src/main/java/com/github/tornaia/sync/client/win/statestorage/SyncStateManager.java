package com.github.tornaia.sync.client.win.statestorage;

import com.github.tornaia.sync.client.win.rest.RestManager;
import com.github.tornaia.sync.client.win.rest.httpclient.RecentChangesResponse;
import com.github.tornaia.sync.client.win.watchservice.DiskWatchService;
import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

@Component
public class SyncStateManager {

    private static final Logger LOG = LoggerFactory.getLogger(SyncStateManager.class);

    @Value("${client.state.file.path:C:\\temp2\\client.db}")
    private String stateFilePath;

    @Autowired
    private RestManager restManager;

    @Autowired
    private DiskWatchService diskWatchService;

    private SyncStateSnapshot syncStateSnapshot;

    @PostConstruct
    public void init() throws ExecutionException, InterruptedException {
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
    public void fetchAllDataSinceLastUpdate() throws ExecutionException, InterruptedException {
        long when = System.currentTimeMillis();
        RecentChangesResponse recentChangesResponse = restManager.getAllAfter(syncStateSnapshot.lastServerInfoAt).get();
        if (recentChangesResponse.status == RecentChangesResponse.Status.TRANSFER_FAILED) {
            LOG.warn("Client is offline! Cannot get updates from server!");
            return;
        }

        recentChangesResponse.fileMetaInfos.forEach(this::fetch);

        syncStateSnapshot.lastServerInfoAt = when;
        writeSyncClientStateToDisk();
    }

    public void fetch(FileMetaInfo newFileMetaInfo) {
        FileMetaInfo oldFileMetaInfo = syncStateSnapshot.get(newFileMetaInfo.relativePath);
        if (newFileMetaInfo.equals(oldFileMetaInfo)) {
            if (Objects.isNull(oldFileMetaInfo.id)) {
                syncStateSnapshot.replace(oldFileMetaInfo, newFileMetaInfo);
                LOG.info("SyncStateSnapshot updated. Old: " + oldFileMetaInfo + ", new: " + newFileMetaInfo);
                return;
            } else {
                LOG.info("SyncStateSnapshot already knew about. Old: " + oldFileMetaInfo + ", new: " + newFileMetaInfo);
            }
            return;
        }
        if (diskWatchService.isFileOnDisk(newFileMetaInfo)) {
            LOG.info("SyncStateSnapshot knows about this file.");
            syncStateSnapshot.put(newFileMetaInfo);
            return;
        }

        restManager.getFile(newFileMetaInfo).thenAccept(fileContent -> {
            diskWatchService.writeToDisk(newFileMetaInfo, fileContent);
            syncStateSnapshot.put(newFileMetaInfo);
        });
    }

    public FileMetaInfo getFileMetaInfo(String relativePath) {
        return syncStateSnapshot.get(relativePath);
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
