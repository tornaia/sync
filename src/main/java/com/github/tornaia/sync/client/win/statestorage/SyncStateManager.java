package com.github.tornaia.sync.client.win.statestorage;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.springframework.stereotype.Component;

import java.io.*;

@Component
public class SyncStateManager {

    private static final String STATE_FILE_PATH = "C:\\temp2\\sync-client-win.db";

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
