package com.github.tornaia.sync.client.win.local.reader.event.single;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_WINDOWS;

public class LocalFileModifiedEvent extends AbstractLocalFileEvent {

    private LocalFileModifiedEvent(String relativePath) {
        super(LocalFileEventType.MODIFIED, relativePath);
    }

    public static LocalFileModifiedEvent ofFile(String relativePath) {
        return new LocalFileModifiedEvent(relativePath);
    }

    public static LocalFileModifiedEvent ofDirectory(String relativePath) {
        return new LocalFileModifiedEvent(relativePath + SEPARATOR_WINDOWS + DOT_FILENAME);
    }
}
