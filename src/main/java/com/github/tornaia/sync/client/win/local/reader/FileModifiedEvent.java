package com.github.tornaia.sync.client.win.local.reader;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_WINDOWS;

public class FileModifiedEvent extends LocalFileEvent {

    private FileModifiedEvent(String relativePath) {
        super(LocalEventType.MODIFIED, relativePath);
    }

    public static FileModifiedEvent ofFile(String relativePath) {
        return new FileModifiedEvent(relativePath);
    }

    public static FileModifiedEvent ofDirectory(String relativePath) {
        return new FileModifiedEvent(relativePath + SEPARATOR_WINDOWS + DOT_FILENAME);
    }
}
