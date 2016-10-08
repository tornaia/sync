package com.github.tornaia.sync.client.win.local.reader;

public class FileCreatedEvent extends LocalFileEvent {

    public FileCreatedEvent(String relativePath) {
        super(LocalEventType.CREATED, relativePath);
    }
}
