package com.github.tornaia.sync.client.win.local.reader.event.single;

import com.github.tornaia.sync.client.win.local.reader.event.LocalFileEventType;

public class LocalFileModifiedEvent extends AbstractLocalFileEvent {

    public LocalFileModifiedEvent(String relativePath) {
        super(LocalFileEventType.MODIFIED, relativePath);
    }
}
