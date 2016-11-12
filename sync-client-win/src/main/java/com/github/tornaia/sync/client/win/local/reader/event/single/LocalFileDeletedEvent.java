package com.github.tornaia.sync.client.win.local.reader.event.single;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;

public class LocalFileDeletedEvent extends AbstractLocalFileEvent {

    public LocalFileDeletedEvent(String relativePath) {
        super(LocalFileEventType.DELETED, relativePath);
    }
}
