package com.github.tornaia.sync.client.win.local.reader.event.single;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;

import static com.github.tornaia.sync.shared.constant.FileSystemConstants.DOT_FILENAME;
import static com.github.tornaia.sync.shared.constant.FileSystemConstants.SEPARATOR_WINDOWS;

public class LocalFileCreatedEvent extends AbstractLocalFileEvent {

    private LocalFileCreatedEvent(String relativePath) {
        super(LocalFileEventType.CREATED, relativePath);
    }

    public static LocalFileCreatedEvent ofFile(String relativePath) {
        return new LocalFileCreatedEvent(relativePath);
    }

    public static LocalFileCreatedEvent ofDirectory(String relativePath) {
        return new LocalFileCreatedEvent(relativePath + SEPARATOR_WINDOWS + DOT_FILENAME);
    }
}
