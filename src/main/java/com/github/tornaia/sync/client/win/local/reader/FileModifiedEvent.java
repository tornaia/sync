package com.github.tornaia.sync.client.win.local.reader;

public class FileModifiedEvent extends LocalFileEvent {

    public FileModifiedEvent(String relativePath) {
        super(LocalEventType.MODIFIED, relativePath);
    }
}
