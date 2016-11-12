package com.github.tornaia.sync.client.win.local.reader.event.single;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;

public class LocalFileCreatedEvent extends AbstractLocalFileEvent {

    public LocalFileCreatedEvent(String relativePath) {
        super(LocalFileEventType.CREATED, relativePath);
    }
}
