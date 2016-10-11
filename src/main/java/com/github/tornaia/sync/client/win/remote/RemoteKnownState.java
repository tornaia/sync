package com.github.tornaia.sync.client.win.remote;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
public class RemoteKnownState {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteKnownState.class);

    private Set<FileMetaInfo> fileMetaInfos = new HashSet<>();

    public synchronized boolean add(FileMetaInfo fileMetaInfo) {
        Optional<FileMetaInfo> optionalKnownFileMetaInfo = get(fileMetaInfo.relativePath);
        if (optionalKnownFileMetaInfo.isPresent()) {
            FileMetaInfo knownFileMetaInfo = optionalKnownFileMetaInfo.get();
            fileMetaInfos.remove(knownFileMetaInfo);
            LOG.info("Updating fileMetaInfo of relativePath: " + fileMetaInfo.relativePath + ", fileMetaInfo: " + fileMetaInfo);
        }

        return fileMetaInfos.add(fileMetaInfo);
    }

    public synchronized Optional<FileMetaInfo> get(String relativePath) {
        return fileMetaInfos.stream().filter(fmi -> Objects.equals(fmi.relativePath, relativePath)).findFirst();
    }
}
