package com.github.tornaia.sync.client.win.local.reader;

public class FileDeleteEvent extends LocalFileEvent {

    public FileDeleteEvent(String relativePath) {
        super(LocalEventType.DELETED, relativePath);
    }
}
