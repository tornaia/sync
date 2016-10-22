package com.github.tornaia.sync.client.win.remote;

import com.github.tornaia.sync.shared.api.FileMetaInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_WINDOWS;

@Component
public class RemoteKnownState {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteKnownState.class);

    private Map<String, FileMetaInfo> fileMetaInfos = new TreeMap<>();

    public synchronized Optional<FileMetaInfo> get(String relativePath) {
        FileMetaInfo fileMetaInfoAsFile = fileMetaInfos.get(relativePath);
        if (fileMetaInfoAsFile != null) {
            return Optional.of(fileMetaInfoAsFile);
        }

        FileMetaInfo fileMetaInfoAsDirectory = fileMetaInfos.get(relativePath + SEPARATOR_WINDOWS + DOT_FILENAME);
        if (fileMetaInfoAsDirectory != null) {
            return Optional.of(fileMetaInfoAsDirectory);
        }

        return Optional.empty();
    }

    public synchronized List<FileMetaInfo> getAllChildrenOrderedByPathLength(String directoryRelativePath) {
        if (!directoryRelativePath.endsWith(SEPARATOR_WINDOWS + DOT_FILENAME)) {
            throw new IllegalStateException("DirectoryPath expected: " + directoryRelativePath);
        }

        String directoryRelativePathWithSlashWithoutDot = directoryRelativePath.substring(0, directoryRelativePath.length() - 1);
        return fileMetaInfos.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(directoryRelativePathWithSlashWithoutDot))
                .map(entry -> entry.getValue())
                .sorted((fmi1, fmi2) -> Integer.compare(fmi1.relativePath.length(), fmi2.relativePath.length()))
                .collect(Collectors.toList());
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
