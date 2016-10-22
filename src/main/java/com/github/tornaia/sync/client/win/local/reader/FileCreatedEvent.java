package com.github.tornaia.sync.client.win.local.reader;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_WINDOWS;

public class FileCreatedEvent extends LocalFileEvent {

    private FileCreatedEvent(String relativePath) {
        super(LocalEventType.CREATED, relativePath);
    }

    public static FileCreatedEvent ofFile(String relativePath) {
        return new FileCreatedEvent(relativePath);
    }

    public static FileCreatedEvent ofDirectory(String relativePath) {
        return new FileCreatedEvent(relativePath + SEPARATOR_WINDOWS + DOT_FILENAME);
    }
}
