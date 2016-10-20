package com.github.tornaia.sync.client.win.remote;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

@Component
public class RemoteKnownState {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteKnownState.class);

    private Map<String, FileMetaInfo> fileMetaInfos = new TreeMap<>();

    public synchronized Optional<FileMetaInfo> get(String relativePath) {
        FileMetaInfo fileMetaInfo = fileMetaInfos.get(relativePath);
        if (fileMetaInfo == null) {
            return Optional.empty();
        }
        return Optional.of(fileMetaInfo);
    }

    public synchronized void add(FileMetaInfo fileMetaInfo) {
        String relativePath = fileMetaInfo.relativePath;
        FileMetaInfo oldFileMetaInfo = fileMetaInfos.put(relativePath, fileMetaInfo);
        LOG.debug("Updated fileMetaInfo of relativePath: " + oldFileMetaInfo + " with " + fileMetaInfo);
    }

    public synchronized void remove(FileMetaInfo fileMetaInfo) {
        String relativePath = fileMetaInfo.relativePath;
        FileMetaInfo removedFileMetaInfo = fileMetaInfos.remove(relativePath);
        LOG.debug("Removed: " + removedFileMetaInfo);
    }
}
