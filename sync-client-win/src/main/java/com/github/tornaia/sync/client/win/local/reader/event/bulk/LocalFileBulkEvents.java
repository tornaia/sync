package com.github.tornaia.sync.client.win.local.reader.event.bulk;

import com.github.tornaia.sync.client.win.local.reader.event.single.AbstractLocalFileEvent;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class LocalFileBulkEvents<T extends AbstractLocalFileEvent> {

    private static final int MAX_NUMBER_OF_EVENTS = 20;
    private static final long PREFERRED_MAX_SIZE = 1 * 1024 * 1024;

    private final Path syncDirectory;
    public List<T> events = new ArrayList<>();

    private long sumSize;

    public LocalFileBulkEvents(Path syncDirectory) {
        this.syncDirectory = syncDirectory;
    }

    public synchronized boolean add(T localFileEvent) {
        if (events.isEmpty()) {
            events.add(localFileEvent);
            return true;
        }

        if (events.size() >= MAX_NUMBER_OF_EVENTS) {
            return false;
        }

        String relativePath = localFileEvent.relativePath;
        long size = getSize(relativePath);
        if (sumSize + size > PREFERRED_MAX_SIZE) {
            return false;
        }

        sumSize += size;
        events.add(localFileEvent);
        return true;
    }

    protected long getSize(String relativePath) {
        Path resolve = syncDirectory.resolve(relativePath);

        File file = resolve.toFile();
        boolean exist = file.exists();
        if (!exist) {
            return Long.MAX_VALUE;
        }

        return file.length();
    }
}
