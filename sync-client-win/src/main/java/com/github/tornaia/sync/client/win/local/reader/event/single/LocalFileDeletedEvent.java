package com.github.tornaia.sync.client.win.local.reader.event.single;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_WINDOWS;

public class LocalFileDeletedEvent extends AbstractLocalFileEvent {

    private LocalFileDeletedEvent(String relativePath) {
        super(LocalFileEventType.DELETED, relativePath);
    }

    public static LocalFileDeletedEvent ofFile(String relativePath) {
        return new LocalFileDeletedEvent(relativePath);
    }

    public static LocalFileDeletedEvent ofDirectory(String relativePath) {
        return new LocalFileDeletedEvent(relativePath + SEPARATOR_WINDOWS + DOT_FILENAME);
    }
}
