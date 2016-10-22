package com.github.tornaia.sync.client.win.local.reader;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_WINDOWS;

public class FileDeleteEvent extends LocalFileEvent {

    private FileDeleteEvent(String relativePath) {
        super(LocalEventType.DELETED, relativePath);
    }

    public static FileDeleteEvent ofFile(String relativePath) {
        return new FileDeleteEvent(relativePath);
    }

    public static FileDeleteEvent ofDirectory(String relativePath) {
        return new FileDeleteEvent(relativePath + SEPARATOR_WINDOWS + DOT_FILENAME);
    }
}
