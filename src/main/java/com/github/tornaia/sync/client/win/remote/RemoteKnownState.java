package com.github.tornaia.sync.client.win.remote;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class RemoteKnownState {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteKnownState.class);

    private Set<FileMetaInfo> fileMetaInfos = new HashSet<>();

    public synchronized boolean add(FileMetaInfo fileMetaInfo) {
        return fileMetaInfos.add(fileMetaInfo);
    }

    public synchronized boolean remove(FileMetaInfo fileMetaInfo) {
        boolean removed = fileMetaInfos.remove(fileMetaInfo);
        if (!removed) {
            LOG.warn("Removing a non existing fileMetaInfo: " + fileMetaInfo);
        }
        return removed;
    }

    public synchronized boolean isKnown(FileMetaInfo what) {
        return fileMetaInfos.stream().filter(fmi -> Objects.equals(fmi, what)).findFirst().isPresent();
    }
}
