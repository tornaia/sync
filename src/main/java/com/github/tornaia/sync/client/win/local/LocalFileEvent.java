package com.github.tornaia.sync.client.win.local;

public abstract class LocalFileEvent {

    public final String relativePath;

    public LocalFileEvent(String relativePath) {
        this.relativePath = relativePath;
    }
}
